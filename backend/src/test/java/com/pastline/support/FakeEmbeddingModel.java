package com.pastline.support;

import com.pastline.ai.exception.EmbeddingUnavailableException;
import com.pastline.ai.port.EmbeddingModelPort;
import java.util.List;
import java.util.Locale;

/**
 * Детермінована заміна Ollama: "мішок слів", кожне слово дає одиницю в позиції hash(слово) mod 768. Спільні слова
 * дають близькі вектори. Перевіряє механіку (індексація, застарілі вектори, ізоляція), а не якість моделі: якість
 * міряє бенчмарк на справжній моделі.
 */
public class FakeEmbeddingModel implements EmbeddingModelPort {

    /** Документ із цим маркером модель "не може" обробити. */
    public static final String FAIL_MARKER = "zzfailzz";

    private volatile boolean available = true;

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String modelName() {
        return "fake-bow";
    }

    @Override
    public float[] embedQuery(String query) {
        checkAvailable();
        return vector(query);
    }

    @Override
    public List<float[]> embedDocuments(List<EmbeddingDocument> documents) {
        checkAvailable();
        return documents.stream()
                .map(d -> {
                    if (d.content().contains(FAIL_MARKER)) {
                        throw new IllegalStateException("model rejected input");
                    }
                    return vector((d.title() == null ? "" : d.title()) + " " + d.content());
                })
                .toList();
    }

    private void checkAvailable() {
        if (!available) {
            throw new EmbeddingUnavailableException();
        }
    }

    private static float[] vector(String text) {
        float[] v = new float[768];
        v[0] = 0.01f; // ненульовий вектор навіть для порожнього тексту: косинус визначений
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (!token.isEmpty()) {
                v[1 + Math.floorMod(token.hashCode(), 767)] += 1f;
            }
        }
        return v;
    }
}
