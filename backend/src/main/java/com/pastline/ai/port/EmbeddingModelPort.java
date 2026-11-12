package com.pastline.ai.port;

import java.util.List;

/**
 * Межа між доменом і моделлю ембедінгів. Пошук не знає ні про Ollama, ні про Spring AI, ні про префікси конкретної
 * моделі: замінити модель (або підставити фейк у тестах) можна без змін у search.
 */
public interface EmbeddingModelPort {

    /** Записується разом із вектором: вектори різних моделей не порівнюються. */
    String modelName();

    float[] embedQuery(String query);

    /** Порядок результатів відповідає порядку документів. */
    List<float[]> embedDocuments(List<EmbeddingDocument> documents);

    record EmbeddingDocument(String title, String content) {}
}
