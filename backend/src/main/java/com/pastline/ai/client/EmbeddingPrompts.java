package com.pastline.ai.client;

/** Префікси запиту й документа, з якими навчали EmbeddingGemma (див. її model card). */
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
