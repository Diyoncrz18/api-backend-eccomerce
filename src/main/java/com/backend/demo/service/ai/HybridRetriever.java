package com.backend.demo.service.ai;

import com.backend.demo.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Combines the semantic retriever (Gemini embeddings / cosine similarity) with
 * the legacy keyword retriever (category synonyms + budget parser + stopwords)
 * using weighted rank-fusion.
 *
 * Final score for a product:
 * <pre>
 *   final = semanticWeight × cosine_normalized
 *         + keywordWeight  × reciprocal_rank_in_keyword_results
 * </pre>
 *
 * Notes:
 * - Cosine is normalized from [-1, 1] → [0, 1] via {@code (cos + 1) / 2}.
 * - Reciprocal rank: 1st = 1.0, 2nd ≈ 0.63, 3rd ≈ 0.5, … (1 / (1 + rank)).
 * - Union of top-N candidates from both retrievers is merged; products only
 *   present in one retriever get 0 for the missing side.
 * - Graceful degradation:
 *   • If semantic is NOT ready → pure keyword.
 *   • If keyword throws       → pure semantic.
 *   • If both fail            → empty list (caller fallback kicks in).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridRetriever {

    private final SemanticRetriever semanticRetriever;
    private final ProductRetriever keywordRetriever; // legacy Phase-1 retriever
    private final EmbeddingService embeddingService;

    @Value("${rag.retrieval.semantic-weight}")
    private double semanticWeight;

    @Value("${rag.retrieval.keyword-weight}")
    private double keywordWeight;

    /**
     * Public entry point used by RagService.
     * Returns the top-K most relevant Product objects (ordered by final hybrid score).
     */
    public List<Product> findRelevant(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) return List.of();

        boolean semanticReady = embeddingService.isReady();
        int candidatePool = Math.max(topK * 3, 12); // over-fetch to give rerank room

        // 1. Get keyword results (always available — has no external dependency)
        List<Product> keywordResults;
        try {
            keywordResults = keywordRetriever.findRelevant(query, candidatePool);
        } catch (Exception e) {
            log.warn("Keyword retriever failed: {}", e.getMessage());
            keywordResults = List.of();
        }

        // 2. Get semantic results if available
        List<SemanticRetriever.ScoredProduct> semanticResults = Collections.emptyList();
        if (semanticReady) {
            try {
                semanticResults = semanticRetriever.findRelevant(query, candidatePool);
            } catch (Exception e) {
                log.warn("Semantic retriever failed: {}", e.getMessage());
            }
        }

        // 3. Degenerate cases — return whatever we have
        if (semanticResults.isEmpty() && keywordResults.isEmpty()) {
            return List.of();
        }
        if (semanticResults.isEmpty()) {
            log.debug("Hybrid: keyword-only path (semantic not ready or failed)");
            return keywordResults.stream().limit(topK).toList();
        }
        if (keywordResults.isEmpty()) {
            log.debug("Hybrid: semantic-only path (keyword returned nothing)");
            return semanticResults.stream()
                .limit(topK)
                .map(SemanticRetriever.ScoredProduct::product)
                .toList();
        }

        // 4. Merge with weighted rank fusion
        Map<Long, Product> productById = new HashMap<>();
        Map<Long, Double> combined = new HashMap<>();

        // semantic contribution
        for (SemanticRetriever.ScoredProduct sp : semanticResults) {
            Long id = sp.product().getId();
            productById.put(id, sp.product());
            // normalize cosine from [-1,1] to [0,1]
            double semNorm = Math.max(0.0, Math.min(1.0, (sp.score() + 1.0) / 2.0));
            combined.merge(id, semanticWeight * semNorm, Double::sum);
        }

        // keyword contribution — reciprocal rank
        for (int rank = 0; rank < keywordResults.size(); rank++) {
            Product p = keywordResults.get(rank);
            Long id = p.getId();
            productById.put(id, p);
            double kwScore = 1.0 / (1.0 + rank); // 1.0, 0.5, 0.33, 0.25, ...
            combined.merge(id, keywordWeight * kwScore, Double::sum);
        }

        // 5. Sort by combined score desc, resolve back to Product, take top-K
        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(combined.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Product> out = new ArrayList<>(topK);
        for (Map.Entry<Long, Double> e : sorted) {
            Product p = productById.get(e.getKey());
            if (p != null) {
                out.add(p);
                if (out.size() >= topK) break;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Hybrid retrieval: semantic={} keyword={} merged={} returning top-{}",
                semanticResults.size(), keywordResults.size(), combined.size(), out.size());
        }
        return out;
    }
}
