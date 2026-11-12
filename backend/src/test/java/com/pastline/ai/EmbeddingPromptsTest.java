package com.pastline.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.pastline.ai.client.EmbeddingPrompts;
import org.junit.jupiter.api.Test;

class EmbeddingPromptsTest {

    @Test
    void queryUsesSearchResultTask() {
        assertThat(EmbeddingPrompts.query("де я бігав")).isEqualTo("task: search result | query: де я бігав");
    }

    @Test
    void documentUsesTitleOrNone() {
        assertThat(EmbeddingPrompts.document("Спорт", "Пробіжка.")).isEqualTo("title: Спорт | text: Пробіжка.");
        assertThat(EmbeddingPrompts.document(null, "Пробіжка.")).isEqualTo("title: none | text: Пробіжка.");
        assertThat(EmbeddingPrompts.document("  ", "Пробіжка.")).isEqualTo("title: none | text: Пробіжка.");
    }
}
