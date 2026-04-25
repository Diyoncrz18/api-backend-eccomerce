package com.backend.demo.service.ai;

import com.backend.demo.model.Product;
import com.backend.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Runs once after the application is fully started. For every active product
 * that is either missing an embedding or whose canonical text hash differs
 * from what is currently stored, enqueues a fresh Gemini embed request.
 *
 * Runs ASYNCHRONOUSLY so startup is not blocked — the app is usable as soon
 * as Spring Boot reports READY. While embeddings are being computed, the
 * retriever gracefully falls back to keyword retrieval.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingBootstrap {

    private final ProductRepository productRepository;
    private final EmbeddingService embeddingService;
    private final EmbeddingClient embeddingClient;

    @Value("${rag.embedding.enabled}")
    private boolean enabled;

    @Value("${rag.embedding.bootstrap-on-startup}")
    private boolean bootstrapOnStartup;

    @Value("${rag.embedding.batch-size}")
    private int batchSize;

    /**
     * Called by Spring when the app is ready. The @Async + spawned thread below
     * means the bootstrap does NOT block the main startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order // default order = lowest priority, runs after everything else
    public void onAppReady() {
        if (!enabled || !bootstrapOnStartup) {
            log.info("Embedding bootstrap skipped (enabled={}, bootstrap={})",
                enabled, bootstrapOnStartup);
            return;
        }
        if (!embeddingClient.isConfigured()) {
            log.info("Embedding bootstrap skipped — GEMINI_API_KEY not configured");
            return;
        }
        // Fire and forget
        new Thread(this::runBootstrapSafely, "embedding-bootstrap").start();
    }

    /**
     * Also callable on demand (e.g. from an admin endpoint). Returns stats.
     * Marked @Async so admin controller can return immediately while work continues.
     */
    @Async
    public void runAsync() {
        runBootstrapSafely();
    }

    /**
     * Synchronous version — returns a result summary for callers that want feedback.
     */
    public BootstrapResult runSync() {
        return runBootstrapSafely();
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private BootstrapResult runBootstrapSafely() {
        try {
            return runBootstrap();
        } catch (Exception e) {
            log.error("Embedding bootstrap failed: {}", e.getMessage(), e);
            return BootstrapResult.error(e.getMessage());
        }
    }

    private BootstrapResult runBootstrap() {
        long t0 = System.currentTimeMillis();

        // Eager fetch category + collection so we can read them outside the TX
        List<Product> products = productRepository.findAllActiveWithAssociations();

        Map<Long, String> storedHashes = embeddingService.getStoredHashes();

        // Decide which need (re-)embedding
        List<Product> toEmbed = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        int skipped = 0;
        for (Product p : products) {
            String text = embeddingService.buildDocumentText(p);
            String hash = embeddingService.hashText(text);
            String existing = storedHashes.get(p.getId());
            if (hash.equals(existing)) {
                skipped++;
                continue;
            }
            toEmbed.add(p);
            texts.add(text);
            hashes.add(hash);
        }

        if (toEmbed.isEmpty()) {
            log.info("Embedding bootstrap: all {} products already embedded (nothing to do)",
                products.size());
            embeddingService.loadCacheFromDb();
            return BootstrapResult.ok(products.size(), 0, skipped);
        }

        log.info("Embedding bootstrap: {} products need embedding (skipping {} up-to-date)",
            toEmbed.size(), skipped);

        int ok = 0;
        int failed = 0;
        int total = toEmbed.size();

        // Batch loop
        for (int start = 0; start < total; start += batchSize) {
            int end = Math.min(total, start + batchSize);
            List<Product> batchProducts = toEmbed.subList(start, end);
            List<String> batchTexts = texts.subList(start, end);
            List<String> batchHashes = hashes.subList(start, end);

            try {
                List<float[]> vectors = embeddingClient.embedDocumentsBatch(batchTexts);
                for (int i = 0; i < batchProducts.size(); i++) {
                    embeddingService.save(
                        batchProducts.get(i).getId(),
                        batchHashes.get(i),
                        vectors.get(i));
                    ok++;
                }
                log.info("Embedded batch {}/{} ({} products)",
                    (end + batchSize - 1) / batchSize,
                    (total + batchSize - 1) / batchSize,
                    batchProducts.size());
            } catch (Exception e) {
                failed += batchProducts.size();
                log.error("Batch embed failed for products {}..{}: {}",
                    start, end - 1, e.getMessage());
                // continue with next batch
            }
        }

        // Reload cache to pick up everything
        embeddingService.loadCacheFromDb();

        long elapsed = System.currentTimeMillis() - t0;
        log.info("Embedding bootstrap DONE in {} ms — embedded={}, failed={}, skipped={}, total={}",
            elapsed, ok, failed, skipped, products.size());

        return new BootstrapResult(true, products.size(), ok, skipped, failed, elapsed, null);
    }

    // ─── Result DTO ─────────────────────────────────────────────────────────

    public record BootstrapResult(
        boolean success,
        int totalProducts,
        int embedded,
        int skipped,
        int failed,
        long elapsedMs,
        String error
    ) {
        static BootstrapResult ok(int total, int embedded, int skipped) {
            return new BootstrapResult(true, total, embedded, skipped, 0, 0L, null);
        }
        static BootstrapResult error(String msg) {
            return new BootstrapResult(false, 0, 0, 0, 0, 0L, msg);
        }
    }
}
