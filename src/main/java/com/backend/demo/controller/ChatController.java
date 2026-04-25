package com.backend.demo.controller;

import com.backend.demo.dto.chat.ChatRequest;
import com.backend.demo.dto.chat.ChatResponse;
import com.backend.demo.service.ai.EmbeddingBootstrap;
import com.backend.demo.service.ai.EmbeddingService;
import com.backend.demo.service.ai.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public chatbot endpoint. Receives a user message + prior chat history,
 * returns a natural-language AI answer plus a list of full product objects.
 *
 * Example request:
 *   POST /api/v1/chat/query
 *   {
 *     "message": "rekomendasi sofa untuk ruang tamu budget 15 juta",
 *     "history": [
 *       { "role": "user", "text": "halo" },
 *       { "role": "ai", "text": "Halo! Saya Maison AI..." }
 *     ]
 *   }
 */
@Slf4j
@RestController
@RequestMapping("${api.prefix}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;
    private final EmbeddingService embeddingService;
    private final EmbeddingBootstrap embeddingBootstrap;

    @PostMapping("/query")
    public ResponseEntity<ChatResponse> query(@Valid @RequestBody ChatRequest request) {
        log.info("Chat query received: len={}, historySize={}",
            request.getMessage().length(),
            request.getHistory() == null ? 0 : request.getHistory().size());

        ChatResponse response = ragService.query(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check that reports which retrieval modes are currently available.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("mode", "rag-chatbot");
        body.put("version", "2.0");
        body.put("embeddingEnabled", embeddingService.isEnabled());
        body.put("embeddingReady", embeddingService.isReady());
        body.put("embeddingCacheSize", embeddingService.cacheSize());
        return ResponseEntity.ok(body);
    }

    /**
     * Admin-only: trigger a (re-)embedding bootstrap. Runs synchronously
     * and returns a summary. Useful after:
     *  - Adding/editing products
     *  - Rotating the Gemini API key
     *  - Changing the embedding model
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmbeddingBootstrap.BootstrapResult> reindex() {
        log.info("Chat reindex triggered (admin)");
        return ResponseEntity.ok(embeddingBootstrap.runSync());
    }
}
