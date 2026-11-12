package com.pastline.ai.client;

import com.pastline.ai.exception.EmbeddingUnavailableException;
import com.pastline.ai.port.EmbeddingModelPort;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

/** Адаптер порту до Spring AI (Ollama, embeddinggemma). */
@Component
public class OllamaEmbeddingAdapter implements EmbeddingModelPort {

    public static final int DIMENSIONS = 768;

    private final EmbeddingModel embeddingModel;
    private final String modelName;
    private final boolean usePrompts;

    public OllamaEmbeddingAdapter(
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.ollama.embedding.model}") String modelName,
            @Value("${pastline.embedding.use-prompts:true}") boolean usePrompts) {
        this.embeddingModel = embeddingModel;
        // Префікси змінюють вектори, тож вони частина "моделі": зміна вмикає перерахунок
        this.modelName = usePrompts ? modelName + "+prompts" : modelName;
        this.usePrompts = usePrompts;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public float[] embedQuery(String query) {
        return checked(call(List.of(usePrompts ? EmbeddingPrompts.query(query) : query)))
                .getFirst();
    }

    @Override
    public List<float[]> embedDocuments(List<EmbeddingDocument> documents) {
        List<String> texts = documents.stream()
                .map(d -> usePrompts ? EmbeddingPrompts.document(d.title(), d.content()) : d.content())
                .toList();
        return checked(call(texts));
    }

    private List<float[]> call(List<String> texts) {
        try {
            return embeddingModel.embed(texts);
        } catch (ResourceAccessException e) {
            // I/O-помилка RestClient: Ollama не відповідає
            throw new EmbeddingUnavailableException();
        }
    }

    private static List<float[]> checked(List<float[]> vectors) {
        for (float[] v : vectors) {
            if (v.length != DIMENSIONS) {
                throw new IllegalStateException(
                        "Очікувалась розмірність " + DIMENSIONS + ", модель повернула " + v.length);
            }
        }
        return vectors;
    }
}
