package com.backend.demo.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over Google Gemini REST API (v1beta).
 *
 * Endpoint pattern:
 *   POST {baseUrl}/models/{model}:generateContent?key={apiKey}
 *
 * Docs: https://ai.google.dev/api/generate-content
 */
@Slf4j
@Component
public class GeminiClient {

    @Value("${rag.gemini.api-key:}")
    private String apiKey;

    @Value("${rag.gemini.base-url}")
    private String baseUrl;

    @Value("${rag.gemini.model}")
    private String model;

    @Value("${rag.gemini.timeout-ms}")
    private long timeoutMs;

    @Value("${rag.gemini.temperature}")
    private double temperature;

    @Value("${rag.gemini.max-output-tokens}")
    private int maxOutputTokens;

    @Value("${rag.gemini.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${rag.gemini.retry.initial-backoff-ms:500}")
    private long retryInitialBackoffMs;

    /**
     * Thinking budget for Gemini 2.5 models. 0 disables "thinking" so all output tokens
     * are spent on the final answer (recommended for short chat responses).
     * -1 = model decides. >0 = max thinking tokens.
     */
    @Value("${rag.gemini.thinking-budget:0}")
    private int thinkingBudget;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2 MB
            .build();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️  GEMINI_API_KEY belum diset — chatbot akan fallback ke mode statis. " +
                     "Set di .env file untuk mengaktifkan RAG.");
        } else {
            log.info("✅ GeminiClient initialized (model={}, timeout={}ms)", model, timeoutMs);
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Call Gemini's generateContent with a plain-text prompt.
     * Returns a GeminiResult containing the text + usage metadata.
     * Throws GeminiException on any failure (caller should fallback gracefully).
     */
    public GeminiResult generate(String prompt) {
        if (!isConfigured()) {
            throw new GeminiException("GEMINI_API_KEY not configured");
        }

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);
        // Gemini 2.5 Flash "thinking" mode consumes output tokens — disable for chat.
        if (thinkingBudget >= 0) {
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", thinkingBudget));
        }

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            )),
            "generationConfig", generationConfig
        );

        String path = "/models/" + model + ":generateContent";
        try {
            GeminiApiResponse apiResponse = webClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).queryParam("key", apiKey).build())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(GeminiApiResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry
                    .backoff(Math.max(0, retryMaxAttempts - 1),
                             Duration.ofMillis(retryInitialBackoffMs))
                    .filter(GeminiClient::isTransient)
                    .doBeforeRetry(sig -> log.warn(
                        "Gemini transient error (attempt {}/{}): {} — retrying",
                        sig.totalRetries() + 1, retryMaxAttempts,
                        sig.failure().getMessage())))
                .block();

            if (apiResponse == null || apiResponse.candidates == null || apiResponse.candidates.isEmpty()) {
                throw new GeminiException("Empty response from Gemini");
            }

            Candidate first = apiResponse.candidates.get(0);
            String text = extractText(first);

            GeminiResult result = new GeminiResult();
            result.setText(text);
            if (apiResponse.usageMetadata != null) {
                result.setPromptTokens(apiResponse.usageMetadata.promptTokenCount);
                result.setResponseTokens(apiResponse.usageMetadata.candidatesTokenCount);
                result.setTotalTokens(apiResponse.usageMetadata.totalTokenCount);
            }
            return result;

        } catch (Exception e) {
            // Retry-exhausted errors from reactor wrap the original cause — unwrap it
            Throwable cause = Exceptions.unwrap(e);
            if (cause instanceof WebClientResponseException wre) {
                log.error("Gemini API error {}: {}", wre.getStatusCode(),
                    wre.getResponseBodyAsString());
                throw new GeminiException("Gemini API error: " + wre.getStatusCode(), wre);
            }
            log.error("Gemini call failed: {}", cause.getMessage());
            throw new GeminiException("Gemini call failed: " + cause.getMessage(), cause);
        }
    }

    /**
     * Returns true only for retryable errors from the upstream API.
     * Transient statuses: 408 (Timeout), 425 (Too Early), 429 (Rate Limit),
     * 500 (Internal), 502 (Bad Gateway), 503 (Service Unavailable), 504 (Gateway Timeout).
     * Also retries on network-level I/O timeouts (reactor's ReadTimeoutException).
     */
    private static boolean isTransient(Throwable t) {
        if (t instanceof WebClientResponseException wex) {
            HttpStatusCode s = wex.getStatusCode();
            int code = s.value();
            return code == 408 || code == 425 || code == 429
                || code == 500 || code == 502 || code == 503 || code == 504;
        }
        // timeouts + reset peer + refused connection etc.
        String cn = t.getClass().getSimpleName();
        return cn.contains("Timeout") || cn.contains("Connect");
    }

    private String extractText(Candidate candidate) {
        if (candidate == null || candidate.content == null
                || candidate.content.parts == null || candidate.content.parts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Part p : candidate.content.parts) {
            if (p.text != null) sb.append(p.text);
        }
        return sb.toString().trim();
    }

    // ─── Result & Exception ──────────────────────────────────────────────────

    @Data
    public static class GeminiResult {
        private String text;
        private Integer promptTokens;
        private Integer responseTokens;
        private Integer totalTokens;
    }

    public static class GeminiException extends RuntimeException {
        public GeminiException(String msg) { super(msg); }
        public GeminiException(String msg, Throwable cause) { super(msg, cause); }
    }

    // ─── Internal API response mapping ───────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiApiResponse {
        private List<Candidate> candidates;
        private UsageMetadata usageMetadata;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Candidate {
        private Content content;
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Content {
        private List<Part> parts;
        private String role;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Part {
        private String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UsageMetadata {
        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;
    }
}
