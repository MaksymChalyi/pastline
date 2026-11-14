package com.pastline.ai.client.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Мінімальний REST-клієнт Gemini: пакетні ембедінги і генерація тексту, без окремого SDK. */
@Component
@ConditionalOnProperty(name = "pastline.ai.provider", havingValue = "gemini")
public class GeminiApi {

    private final RestClient rest;

    @Autowired
    public GeminiApi(
            @Value("${pastline.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${pastline.gemini.api-key:}") String apiKey,
            @Value("${pastline.gemini.timeout:60s}") Duration timeout) {
        this(build(baseUrl, apiKey, timeout));
    }

    GeminiApi(RestClient rest) {
        this.rest = rest;
    }

    private static RestClient build(String baseUrl, String apiKey, Duration timeout) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("pastline.ai.provider=gemini, але GEMINI_API_KEY не задано");
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    /** Опис помилки з тіла відповіді Gemini для логу (без тексту запиту), до 300 символів. */
    public static String errorMessage(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        String oneLine = body.replaceAll("\\s+", " ").strip();
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "…" : oneLine;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(String role, List<Part> parts) {
        static Content of(String role, String text) {
            return new Content(role, List.of(new Part(text)));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EmbedRequest(
            String model, Content content, String taskType, String title, Integer outputDimensionality) {}

    record BatchEmbedRequest(List<EmbedRequest> requests) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Embedding(List<Float> values) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BatchEmbedResponse(List<Embedding> embeddings) {}

    /** Порядок векторів відповідає порядку запитів. */
    public List<float[]> batchEmbed(String model, List<EmbedRequest> requests) {
        BatchEmbedResponse response = rest.post()
                .uri("/models/{model}:batchEmbedContents", model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BatchEmbedRequest(requests))
                .retrieve()
                .body(BatchEmbedResponse.class);
        if (response == null
                || response.embeddings() == null
                || response.embeddings().size() != requests.size()) {
            throw new IllegalStateException("Gemini повернув неповну відповідь на ембедінги");
        }
        return response.embeddings().stream().map(GeminiApi::toArray).toList();
    }

    private static float[] toArray(Embedding e) {
        float[] v = new float[e.values().size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = e.values().get(i);
        }
        return v;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerationConfig(Double temperature, Integer maxOutputTokens) {}

    record GenerateRequest(Content systemInstruction, List<Content> contents, GenerationConfig generationConfig) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates) {}

    /** Текст першого кандидата; порожній рядок, якщо модель нічого не повернула (наприклад, спрацював фільтр). */
    public String generate(String model, String system, String user, double temperature, int maxTokens) {
        GenerateResponse response = rest.post()
                .uri("/models/{model}:generateContent", model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GenerateRequest(
                        Content.of(null, system),
                        List.of(Content.of("user", user)),
                        new GenerationConfig(temperature, maxTokens)))
                .retrieve()
                .body(GenerateResponse.class);
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            return "";
        }
        Content content = response.candidates().getFirst().content();
        if (content == null || content.parts() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        content.parts().forEach(p -> sb.append(p.text() == null ? "" : p.text()));
        return sb.toString().strip();
    }
}
