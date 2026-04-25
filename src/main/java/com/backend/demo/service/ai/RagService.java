package com.backend.demo.service.ai;

import com.backend.demo.dto.ProductResponse;
import com.backend.demo.dto.chat.ChatMessage;
import com.backend.demo.dto.chat.ChatRequest;
import com.backend.demo.dto.chat.ChatResponse;
import com.backend.demo.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the RAG flow: retrieval → prompt assembly → LLM call
 * → response parsing → product resolution.
 *
 * Designed to degrade gracefully: if Gemini is unavailable or fails,
 * falls back to a heuristic response using the retrieved products.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final HybridRetriever retriever;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;

    @Value("${rag.retrieval.top-k:6}")
    private int topK;

    @Value("${rag.history.max-messages:6}")
    private int historyMax;

    // Regex to extract PRODUCT_IDS: [1, 2, 3] marker
    private static final Pattern IDS_MARKER =
        Pattern.compile("PRODUCT_IDS\\s*:\\s*\\[([^\\]]*)\\]", Pattern.CASE_INSENSITIVE);
    // Regex to extract INTENT: xxx marker
    private static final Pattern INTENT_MARKER =
        Pattern.compile("INTENT\\s*:\\s*([a-z_]+)", Pattern.CASE_INSENSITIVE);

    public ChatResponse query(ChatRequest request) {
        String userMessage = request.getMessage();
        List<ChatMessage> history = limitHistory(request.getHistory());

        // 1. Retrieval
        List<Product> retrieved = retriever.findRelevant(userMessage, topK);

        // Index by ID for fast lookup after parsing
        Map<Long, Product> byId = new LinkedHashMap<>();
        for (Product p : retrieved) byId.put(p.getId(), p);

        // 2. If Gemini not configured, skip LLM and build a heuristic response
        if (!geminiClient.isConfigured()) {
            log.info("Gemini not configured — using heuristic fallback");
            return heuristicResponse(userMessage, retrieved);
        }

        // 3. Build prompt & call LLM
        String prompt = promptBuilder.build(userMessage, history, retrieved);

        GeminiClient.GeminiResult llm;
        try {
            llm = geminiClient.generate(prompt);
        } catch (GeminiClient.GeminiException e) {
            log.warn("Gemini call failed, falling back: {}", e.getMessage());
            ChatResponse fb = heuristicResponse(userMessage, retrieved);
            fb.setSuccess(false);
            return fb;
        }

        // 4. Parse response
        String rawText = llm.getText() == null ? "" : llm.getText();
        List<Long> idsFromLLM = extractProductIds(rawText);
        String intent = extractIntent(rawText);
        String cleanText = stripMarkers(rawText);

        // Validate IDs against retrieved pool (prevent hallucination)
        List<Product> finalProducts = new ArrayList<>();
        for (Long id : idsFromLLM) {
            Product p = byId.get(id);
            if (p != null) finalProducts.add(p);
        }

        // 5. Build response DTO
        ChatResponse.Usage usage = ChatResponse.Usage.builder()
            .promptTokens(llm.getPromptTokens())
            .responseTokens(llm.getResponseTokens())
            .totalTokens(llm.getTotalTokens())
            .build();

        return ChatResponse.builder()
            .text(cleanText.isBlank() ? defaultText(retrieved) : cleanText)
            .productIds(finalProducts.stream().map(Product::getId).toList())
            .products(finalProducts.stream().map(ProductResponse::from).toList())
            .intent(intent == null ? "fallback" : intent)
            .usage(usage)
            .success(true)
            .build();
    }

    // ─── Fallback (no LLM) ───────────────────────────────────────────────────

    private ChatResponse heuristicResponse(String userMessage, List<Product> retrieved) {
        List<Product> picked = retrieved.stream().limit(3).toList();

        String text;
        if (picked.isEmpty()) {
            text = "Maaf, saat ini saya belum menemukan produk yang cocok dengan permintaan Anda. " +
                   "Coba sebutkan kategori (Kursi, Meja, Lampu, Dekorasi, Penyimpanan) " +
                   "atau anggaran yang Anda miliki.";
        } else {
            text = "Berikut " + picked.size() + " rekomendasi yang mungkin cocok untuk Anda. " +
                   "Klik salah satu untuk melihat detail lengkapnya.";
        }

        return ChatResponse.builder()
            .text(text)
            .productIds(picked.stream().map(Product::getId).toList())
            .products(picked.stream().map(ProductResponse::from).toList())
            .intent(picked.isEmpty() ? "fallback" : "product_recommendation")
            .success(false)
            .build();
    }

    private String defaultText(List<Product> retrieved) {
        if (retrieved.isEmpty()) {
            return "Bisa Anda ceritakan lebih detail ruangan atau gaya yang Anda inginkan?";
        }
        return "Berikut beberapa rekomendasi dari koleksi Maison untuk Anda.";
    }

    // ─── Parsing helpers ─────────────────────────────────────────────────────

    private List<Long> extractProductIds(String rawText) {
        if (rawText == null) return List.of();
        Matcher m = IDS_MARKER.matcher(rawText);
        if (!m.find()) return List.of();
        String inner = m.group(1).trim();
        if (inner.isEmpty()) return List.of();

        List<Long> ids = new ArrayList<>();
        for (String token : inner.split(",")) {
            String t = token.trim().replaceAll("[^0-9]", "");
            if (t.isEmpty()) continue;
            try {
                ids.add(Long.parseLong(t));
            } catch (NumberFormatException ignore) {
                // skip malformed
            }
        }
        return ids;
    }

    private String extractIntent(String rawText) {
        if (rawText == null) return null;
        Matcher m = INTENT_MARKER.matcher(rawText);
        if (m.find()) return m.group(1).toLowerCase(Locale.ROOT);
        return null;
    }

    /** Remove INTENT: and PRODUCT_IDS: marker lines from the visible text. */
    private String stripMarkers(String rawText) {
        if (rawText == null) return "";
        String out = rawText;
        out = IDS_MARKER.matcher(out).replaceAll("");
        out = INTENT_MARKER.matcher(out).replaceAll("");
        // remove any orphan trailing "INTENT" / "PRODUCT_IDS" words + excess blank lines
        out = out.replaceAll("(?im)^\\s*(intent|product_ids)\\s*:?.*$", "");
        out = out.replaceAll("\\n{3,}", "\n\n");
        return out.trim();
    }

    private List<ChatMessage> limitHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return List.of();
        int size = history.size();
        if (size <= historyMax) return history;
        return history.subList(size - historyMax, size);
    }
}
