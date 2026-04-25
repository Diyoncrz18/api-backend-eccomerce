package com.backend.demo.dto.chat;

import com.backend.demo.dto.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response body returned by the RAG chat endpoint.
 * Contains the natural-language answer plus full product objects
 * that the AI referenced (ready to render as product cards).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** Clean natural language answer (markers like "PRODUCT_IDS:" removed) */
    private String text;

    /** Product IDs the AI selected, in recommendation order */
    @Builder.Default
    private List<Long> productIds = new ArrayList<>();

    /** Full product objects (resolved from IDs) — ready for UI cards */
    @Builder.Default
    private List<ProductResponse> products = new ArrayList<>();

    /** High-level intent classification (greeting, product_recommendation, off_topic, fallback) */
    private String intent;

    /** Token usage info (for debugging / cost monitoring) */
    private Usage usage;

    /** True when Gemini call succeeded; false when we returned a fallback */
    private boolean success;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private Integer promptTokens;
        private Integer responseTokens;
        private Integer totalTokens;
    }
}
