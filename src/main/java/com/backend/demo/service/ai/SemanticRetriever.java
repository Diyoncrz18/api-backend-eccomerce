package com.backend.demo.service.ai;

import com.backend.demo.model.Product;
import com.backend.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Retrieves products by semantic similarity to the user query.
 *
 * Flow:
 *  1. Embed the user query with Gemini (RETRIEVAL_QUERY task type).
 *  2. L2-normalize the query vector.
 *  3. Compute cosine similarity against every cached product vector.
 *  4. Return the top-K products with their similarity scores.
 *
 * Returns an empty list if the service is not ready (no API key, cache empty,
 * or query embedding failed) — caller should fall back to keyword retrieval.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticRetriever {

    private final EmbeddingService embeddingService;
    private final ProductRepository productRepository;

    /**
     * Returns top-K products with their raw cosine score [−1, 1], highest first.
     */
    public List<ScoredProduct> findRelevant(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) return List.of();
        if (!embeddingService.isReady()) {
            log.debug("SemanticRetriever not ready — cache size={}", embeddingService.cacheSize());
            return List.of();
        }

        // 1. Embed query
        float[] queryVec;
        try {
            queryVec = embeddingService.embedQueryNormalized(query);
        } catch (Exception e) {
            log.warn("Query embedding failed: {}", e.getMessage());
            return List.of();
        }

        // 2. Score every cached product
        Map<Long, float[]> cache = embeddingService.getAllVectors();
        if (cache.isEmpty()) return List.of();

        PriorityQueue<ScoredId> heap = new PriorityQueue<>(
            Comparator.comparingDouble(s -> s.score)); // min-heap
        for (Map.Entry<Long, float[]> e : cache.entrySet()) {
            float sim = EmbeddingService.cosine(queryVec, e.getValue());
            if (heap.size() < topK) {
                heap.offer(new ScoredId(e.getKey(), sim));
            } else if (sim > heap.peek().score) {
                heap.poll();
                heap.offer(new ScoredId(e.getKey(), sim));
            }
        }

        // 3. Drain heap, sort by score desc
        List<ScoredId> sortedIds = new ArrayList<>(heap);
        sortedIds.sort((a, b) -> Float.compare(b.score, a.score));

        if (sortedIds.isEmpty()) return List.of();

        // 4. Batch-load products (preserving order) and pair with score
        List<Long> ids = sortedIds.stream().map(s -> s.id).toList();
        Map<Long, Product> byId = new HashMap<>();
        for (Product p : productRepository.findAllById(ids)) {
            byId.put(p.getId(), p);
        }

        List<ScoredProduct> out = new ArrayList<>(sortedIds.size());
        for (ScoredId si : sortedIds) {
            Product p = byId.get(si.id);
            if (p != null && Boolean.TRUE.equals(p.getIsActive())) {
                out.add(new ScoredProduct(p, si.score));
            }
        }
        return out;
    }

    /**
     * Convenience fallback when retrieval infrastructure is unavailable:
     * returns the N highest-rated active products with score=0.
     */
    public List<ScoredProduct> topByRating(int topK) {
        List<Product> pool = productRepository
            .findAllActive(PageRequest.of(0, topK, Sort.by(Sort.Direction.DESC, "rating")))
            .getContent();
        return pool.stream()
            .map(p -> new ScoredProduct(p, 0f))
            .toList();
    }

    // ─── Inner types ─────────────────────────────────────────────────────────

    public record ScoredProduct(Product product, float score) {}

    private record ScoredId(long id, float score) {}
}
