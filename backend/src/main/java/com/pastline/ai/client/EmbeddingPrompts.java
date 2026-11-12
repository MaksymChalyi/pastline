package com.pastline.ai.client;

/**
 * Префікси, з якими навчали EmbeddingGemma (model card: ai.google.dev/gemma/docs/embeddinggemma/model_card).
 * Запит і документ кодуються по-різному, тому модель розрізняє "що шукаю" і "що зберігаю".
 */
public final class EmbeddingPrompts {

    private EmbeddingPrompts() {}

    public static String query(String query) {
        return "task: search result | query: " + query;
    }

    public static String document(String title, String content) {
        String safeTitle = title == null || title.isBlank() ? "none" : title.strip();
        return "title: " + safeTitle + " | text: " + content;
    }
}
