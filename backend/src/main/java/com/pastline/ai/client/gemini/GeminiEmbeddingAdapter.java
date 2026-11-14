package com.pastline.ai.client.gemini;

import com.pastline.ai.client.OllamaEmbeddingAdapter;
import com.pastline.ai.exception.EmbeddingUnavailableException;
import com.pastline.ai.port.EmbeddingModelPort;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Ембедінги через Gemini (онлайн-демо). Розмірність 768, як у схемі БД: gemini-embedding-001 навчено так, що
 * перші 768 координат дають придатний вектор. Запит і документ розрізняються через taskType, а не текстові
 * префікси, як у EmbeddingGemma. Числа експерименту з цією моделлю не порівнюються.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pastline.ai.provider", havingValue = "gemini")
public class GeminiEmbeddingAdapter implements EmbeddingModelPort {

    private final GeminiApi api;
    private final String model;

    public GeminiEmbeddingAdapter(
            GeminiApi api, @Value("${pastline.gemini.embedding-model:gemini-embedding-001}") String model) {
        this.api = api;
        this.model = model;
    }

    @Override
    public String modelName() {
        // Розмірність частина назви: інша розмірність = інші вектори, старі мають перерахуватись
        return model + "@" + OllamaEmbeddingAdapter.DIMENSIONS;
    }

    @Override
    public float[] embedQuery(String query) {
        return call(() -> api.batchEmbed(model, List.of(request(query, "RETRIEVAL_QUERY", null))))
                .getFirst();
    }

    @Override
    public List<float[]> embedDocuments(List<EmbeddingDocument> documents) {
        List<GeminiApi.EmbedRequest> requests = documents.stream()
                .map(d -> request(d.content(), "RETRIEVAL_DOCUMENT", blankToNull(d.title())))
                .toList();
        return call(() -> api.batchEmbed(model, requests));
    }

    private GeminiApi.EmbedRequest request(String text, String taskType, String title) {
        return new GeminiApi.EmbedRequest(
                "models/" + model,
                GeminiApi.Content.of(null, text),
                taskType,
                title,
                OllamaEmbeddingAdapter.DIMENSIONS);
    }

    private static List<float[]> call(Supplier<List<float[]>> request) {
        List<float[]> vectors;
        try {
            vectors = request.get();
        } catch (ResourceAccessException e) {
            throw new EmbeddingUnavailableException();
        } catch (RestClientResponseException e) {
            // 429 (ліміт безкоштовного рівня) і 5xx — тимчасово; у лог лише статус, без тексту записів
            log.warn("Gemini embeddings: HTTP {} {}", e.getStatusCode().value(), GeminiApi.errorMessage(e));
            throw new EmbeddingUnavailableException();
        }
        for (float[] v : vectors) {
            if (v.length != OllamaEmbeddingAdapter.DIMENSIONS) {
                throw new IllegalStateException("Очікувалась розмірність " + OllamaEmbeddingAdapter.DIMENSIONS
                        + ", Gemini повернув " + v.length);
            }
        }
        return vectors;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
