package com.pastline.ai.port;

import java.util.List;

/** Межа між доменом і моделлю ембедінгів: пошук не знає, яка модель і який провайдер за нею. */
public interface EmbeddingModelPort {

    /** Записується разом із вектором: вектори різних моделей не порівнюються. */
    String modelName();

    float[] embedQuery(String query);

    /** Порядок результатів відповідає порядку документів. */
    List<float[]> embedDocuments(List<EmbeddingDocument> documents);

    record EmbeddingDocument(String title, String content) {}
}
