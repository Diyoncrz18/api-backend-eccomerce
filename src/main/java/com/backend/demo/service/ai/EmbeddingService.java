package com.backend.demo.service.ai;

import com.backend.demo.model.Product;
import com.backend.demo.model.ProductEmbedding;
import com.backend.demo.repository.ProductEmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service for managing product embeddings.
 *
 * Responsibilities:
 *  - Build the canonical "document text" for each product (the string we embed).
 *  - Detect when a product needs (re-)embedding via SHA-256 hash comparison.
 *  - Persist vectors as JSON in the {@code product_embeddings} table.
 *  - Maintain an in-memory cache {@code Map<productId, normalizedVector>} for fast
 *    cosine-similarity lookups at query time.
 *  - Expose a simple public API for the retriever and bootstrap classes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ProductEmbeddingRepository repository;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;

    @Value("${rag.embedding.enabled}")
    private boolean enabled;

    /** Runtime cache: productId → L2-normalized vector (ready for cosine). */
    private final Map<Long, float[]> vectorCache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("Embedding service disabled (rag.embedding.enabled=false)");
            return;
        }
        try {
            loadCacheFromDb();
        } catch (Exception e) {
            log.warn("Failed to load embedding cache: {}", e.getMessage());
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public boolean isReady() {
        return enabled && embeddingClient.isConfigured() && !vectorCache.isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int cacheSize() {
        return vectorCache.size();
    }

    /**
     * Return an immutable snapshot of the vector cache for iteration at query time.
     */
    public Map<Long, float[]> getAllVectors() {
        return Collections.unmodifiableMap(vectorCache);
    }

    /**
     * Load all embeddings matching the configured model from the DB into memory.
     */
    public void loadCacheFromDb() {
        String model = embeddingClient.getModelName();
        List<ProductEmbedding> rows = repository.findByModel(model);
        vectorCache.clear();
        int bad = 0;
        for (ProductEmbedding row : rows) {
            try {
                float[] v = parseAndNormalize(row.getVectorJson());
                vectorCache.put(row.getProductId(), v);
            } catch (Exception e) {
                bad++;
                log.warn("Corrupt embedding for product {}: {}", row.getProductId(), e.getMessage());
            }
        }
        log.info("Loaded {} embeddings into memory cache (skipped {} corrupt rows, model={})",
            vectorCache.size(), bad, model);
    }

    /**
     * Embed a user query at runtime. Returns a normalized vector ready for cosine.
     * Throws {@link EmbeddingClient.EmbeddingException} on failure.
     */
    public float[] embedQueryNormalized(String query) {
        float[] raw = embeddingClient.embedQuery(query);
        return normalize(raw);
    }

    /**
     * Build the canonical document text used for embedding a product.
     * Includes: name, category, collection, material, description, dimensions.
     * This is what determines the hash → any change here triggers re-embed.
     */
    public String buildDocumentText(Product p) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Produk: ").append(nvl(p.getName())).append(". ");
        if (p.getCategory() != null) {
            sb.append("Kategori: ").append(nvl(p.getCategory().getName())).append(". ");
        }
        if (p.getCollection() != null) {
            sb.append("Koleksi: ").append(nvl(p.getCollection().getName())).append(". ");
        }
        if (p.getMaterial() != null && !p.getMaterial().isBlank()) {
            sb.append("Material: ").append(p.getMaterial()).append(". ");
        }
        if (p.getDimensions() != null && !p.getDimensions().isBlank()) {
            sb.append("Dimensi: ").append(p.getDimensions()).append(". ");
        }
        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            sb.append("Deskripsi: ").append(p.getDescription());
        }
        return sb.toString().trim();
    }

    /** SHA-256 hash (first 16 hex chars = 64 bits, plenty for dedup) */
    public String hashText(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Persist (or update) one embedding. Also updates the in-memory cache.
     * Caller must supply the raw (un-normalized) float vector as returned by Gemini.
     */
    @Transactional
    public void save(Long productId, String hash, float[] rawVector) {
        String json;
        try {
            json = objectMapper.writeValueAsString(rawVector);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize embedding vector", e);
        }
        ProductEmbedding row = repository.findById(productId)
            .orElseGet(() -> ProductEmbedding.builder().productId(productId).build());
        row.setVectorJson(json);
        row.setModel(embeddingClient.getModelName());
        row.setSourceHash(hash);
        row.setDimensions(rawVector.length);
        repository.save(row);

        // update cache with normalized copy
        vectorCache.put(productId, normalize(rawVector));
    }

    /**
     * Returns productIds whose current stored hash still matches the desired hash
     * (so we can skip re-embedding them).
     */
    public Map<Long, String> getStoredHashes() {
        Map<Long, String> out = new HashMap<>();
        for (ProductEmbedding row : repository.findByModel(embeddingClient.getModelName())) {
            out.put(row.getProductId(), row.getSourceHash());
        }
        return out;
    }

    // ─── Math helpers ────────────────────────────────────────────────────────

    /** Returns new array = v / ||v||₂; returns zero-vector if ||v||=0. */
    public static float[] normalize(float[] v) {
        if (v == null || v.length == 0) return new float[0];
        double sum = 0.0;
        for (float x : v) sum += (double) x * x;
        double norm = Math.sqrt(sum);
        if (norm == 0.0) return v.clone();
        float[] out = new float[v.length];
        float inv = (float) (1.0 / norm);
        for (int i = 0; i < v.length; i++) {
            out[i] = v[i] * inv;
        }
        return out;
    }

    /** Cosine similarity. Assumes both vectors are already L2-normalized → reduces to dot. */
    public static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0f;
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
        }
        return (float) dot;
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private float[] parseAndNormalize(String json) throws Exception {
        float[] arr = objectMapper.readValue(json, float[].class);
        return normalize(arr);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
