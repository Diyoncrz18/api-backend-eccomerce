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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps the Gemini Embedding REST API.
 *
 * Endpoints used (v1beta):
 *  - Single embed: POST /models/{model}:embedContent?key=KEY
 *  - Batch embed:  POST /models/{model}:batchEmbedContents?key=KEY
 *
 * Docs: https://ai.google.dev/gemini-api/docs/embeddings
 *
 * Uses:
 *  - taskType=RETRIEVAL_DOCUMENT when embedding catalog entries (stored)
 *  - taskType=RETRIEVAL_QUERY when embedding user queries (runtime)
 *
 * Both task types produce vectors in the same space, optimised for asymmetric
 * retrieval (user intent vs. long product description).
 */
@Slf4j
@Component
public class EmbeddingClient {

    @Value("${rag.gemini.api-key:}")
    private String apiKey;

    @Value("${rag.gemini.base-url}")
    private String baseUrl;

    @Value("${rag.embedding.model}")
    private String embeddingModel;

    @Value("${rag.embedding.dimensions:0}")
    private int outputDimensionality;

    @Value("${rag.gemini.timeout-ms}")
    private long timeoutMs;

    @Value("${rag.gemini.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${rag.gemini.retry.initial-backoff-ms:500}")
    private long retryInitialBackoffMs;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024)) // 8 MB (batch can be big)
            .build();

        if (!isConfigured()) {
            log.warn("EmbeddingClient: GEMINI_API_KEY not set — semantic retrieval disabled");
        } else {
            log.info("EmbeddingClient initialized (model={}, dims={})",
                embeddingModel, outputDimensionality > 0 ? outputDimensionality : "native");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getModelName() {
        return embeddingModel;
    }

    /**
     * Embed a single piece of text. Use RETRIEVAL_QUERY at runtime for user queries.
     */
    public float[] embedQuery(String text) {
        return embedSingle(text, "RETRIEVAL_QUERY");
    }

    /**
     * Embed a single document. Use at ingest time (runtime for a single re-embed).
     */
    public float[] embedDocument(String text) {
        return embedSingle(text, "RETRIEVAL_DOCUMENT");
    }

    /**
     * Batch-embed documents. Returns vectors in the same order as the input list.
     */
    public List<float[]> embedDocumentsBatch(List<String> texts) {
        if (!isConfigured()) {
            throw new EmbeddingException("GEMINI_API_KEY not configured");
        }
        if (texts == null || texts.isEmpty()) return List.of();

        List<Map<String, Object>> requests = new ArrayList<>(texts.size());
        String modelPath = "models/" + embeddingModel;
        for (String t : texts) {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("model", modelPath);
            req.put("content", Map.of("parts", List.of(Map.of("text", safe(t)))));
            req.put("taskType", "RETRIEVAL_DOCUMENT");
            if (outputDimensionality > 0) {
                req.put("outputDimensionality", outputDimensionality);
            }
            requests.add(req);
        }

        Map<String, Object> body = Map.of("requests", requests);
        String path = "/models/" + embeddingModel + ":batchEmbedContents";

        try {
            BatchEmbedResponse resp = webClient.post()
                .uri(uri -> uri.path(path).queryParam("key", apiKey).build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(BatchEmbedResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(buildRetry("batch embed"))
                .block();

            if (resp == null || resp.embeddings == null || resp.embeddings.size() != texts.size()) {
                int got = (resp == null || resp.embeddings == null) ? 0 : resp.embeddings.size();
                throw new EmbeddingException(
                    "Batch embed returned " + got + " vectors but expected " + texts.size());
            }

            List<float[]> out = new ArrayList<>(texts.size());
            for (EmbedEntry e : resp.embeddings) {
                out.add(toFloatArray(e.values));
            }
            return out;

        } catch (Exception e) {
            if (e instanceof EmbeddingException ee) throw ee;
            Throwable cause = Exceptions.unwrap(e);
            if (cause instanceof WebClientResponseException wre) {
                log.error("Gemini batch embed API error {}: {}", wre.getStatusCode(),
                    wre.getResponseBodyAsString());
                throw new EmbeddingException("Batch embed failed: " + wre.getStatusCode(), wre);
            }
            log.error("Batch embed error: {}", cause.getMessage());
            throw new EmbeddingException("Batch embed failed: " + cause.getMessage(), cause);
        }
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private float[] embedSingle(String text, String taskType) {
        if (!isConfigured()) {
            throw new EmbeddingException("GEMINI_API_KEY not configured");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "models/" + embeddingModel);
        body.put("content", Map.of("parts", List.of(Map.of("text", safe(text)))));
        body.put("taskType", taskType);
        if (outputDimensionality > 0) {
            body.put("outputDimensionality", outputDimensionality);
        }
        String path = "/models/" + embeddingModel + ":embedContent";

        try {
            SingleEmbedResponse resp = webClient.post()
                .uri(uri -> uri.path(path).queryParam("key", apiKey).build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SingleEmbedResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(buildRetry("embed"))
                .block();

            if (resp == null || resp.embedding == null || resp.embedding.values == null) {
                throw new EmbeddingException("Empty embed response");
            }
            return toFloatArray(resp.embedding.values);

        } catch (Exception e) {
            if (e instanceof EmbeddingException ee) throw ee;
            Throwable cause = Exceptions.unwrap(e);
            if (cause instanceof WebClientResponseException wre) {
                log.error("Gemini embed API error {}: {}", wre.getStatusCode(),
                    wre.getResponseBodyAsString());
                throw new EmbeddingException("Embed failed: " + wre.getStatusCode(), wre);
            }
            log.error("Embed error: {}", cause.getMessage());
            throw new EmbeddingException("Embed failed: " + cause.getMessage(), cause);
        }
    }

    /**
     * Builds a retry spec that only re-fires on transient upstream errors,
     * using exponential backoff. Shared between batch + single embed calls.
     */
    private Retry buildRetry(String opName) {
        return Retry
            .backoff(Math.max(0, retryMaxAttempts - 1),
                     Duration.ofMillis(retryInitialBackoffMs))
            .filter(EmbeddingClient::isTransient)
            .doBeforeRetry(sig -> log.warn(
                "Gemini {} transient error (attempt {}/{}): {} — retrying",
                opName, sig.totalRetries() + 1, retryMaxAttempts,
                sig.failure().getMessage()));
    }

    private static boolean isTransient(Throwable t) {
        if (t instanceof WebClientResponseException wex) {
            HttpStatusCode s = wex.getStatusCode();
            int code = s.value();
            return code == 408 || code == 425 || code == 429
                || code == 500 || code == 502 || code == 503 || code == 504;
        }
        String cn = t.getClass().getSimpleName();
        return cn.contains("Timeout") || cn.contains("Connect");
    }

    private static float[] toFloatArray(List<Double> values) {
        if (values == null) return new float[0];
        float[] out = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Double v = values.get(i);
            out[i] = v == null ? 0f : v.floatValue();
        }
        return out;
    }

    private static String safe(String s) {
        if (s == null) return "";
        // Gemini rejects very long inputs (>~2048 tokens) — cap to safe size
        return s.length() > 8_000 ? s.substring(0, 8_000) : s;
    }

    // ─── Exception ────────────────────────────────────────────────────────────

    public static class EmbeddingException extends RuntimeException {
        public EmbeddingException(String msg) { super(msg); }
        public EmbeddingException(String msg, Throwable cause) { super(msg, cause); }
    }

    // ─── API response mapping ────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SingleEmbedResponse {
        private EmbedEntry embedding;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BatchEmbedResponse {
        private List<EmbedEntry> embeddings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EmbedEntry {
        private List<Double> values;
    }
}
