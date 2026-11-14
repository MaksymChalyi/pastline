package com.pastline.search.embedding;

import com.pastline.ai.exception.EmbeddingUnavailableException;
import com.pastline.ai.port.EmbeddingModelPort;
import com.pastline.ai.port.EmbeddingModelPort.EmbeddingDocument;
import com.pastline.search.repository.EmbeddingRepository;
import com.pastline.search.repository.EmbeddingRepository.StaleEntry;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Рахує вектори для записів без актуального вектора (немає, змінився текст або модель). */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingIndexer {

    public static final int BATCH_SIZE = 32;
    private static final int MAX_ERROR_LENGTH = 200;

    private final EmbeddingModelPort model;
    private final EmbeddingRepository repository;
    private final Clock clock;

    /** Одна пачка. Повертає кількість оброблених записів (0, якщо роботи немає або модель недоступна). */
    public int indexPending() {
        List<StaleEntry> batch = repository.findStale(model.modelName(), BATCH_SIZE);
        if (batch.isEmpty()) {
            return 0;
        }
        try {
            List<float[]> vectors = model.embedDocuments(
                    batch.stream().map(EmbeddingIndexer::document).toList());
            for (int i = 0; i < batch.size(); i++) {
                save(batch.get(i), vectors.get(i));
            }
            return batch.size();
        } catch (EmbeddingUnavailableException e) {
            log.warn("Модель ембедінгів недоступна, повтор пізніше");
            return 0;
        } catch (RuntimeException e) {
            // Пачка впала: по одному, щоб один проблемний запис не блокував решту
            return indexOneByOne(batch);
        }
    }

    /** Доки є робота. Для тестів і ручної переіндексації; у застосунку працює планувальник. */
    public int indexAll() {
        int total = 0;
        int processed;
        while ((processed = indexPending()) > 0) {
            total += processed;
        }
        return total;
    }

    private int indexOneByOne(List<StaleEntry> batch) {
        int processed = 0;
        for (StaleEntry entry : batch) {
            try {
                save(entry, model.embedDocuments(List.of(document(entry))).getFirst());
            } catch (EmbeddingUnavailableException e) {
                log.warn("Модель ембедінгів недоступна, повтор пізніше");
                return processed;
            } catch (RuntimeException e) {
                // Лише тип і повідомлення помилки, без тексту запису
                log.warn(
                        "Не вдалося порахувати вектор запису {}: {}",
                        entry.id(),
                        e.getClass().getSimpleName());
                repository.saveFailed(entry.id(), model.modelName(), entry.contentHash(), error(e), clock.instant());
            }
            processed++;
        }
        return processed;
    }

    private void save(StaleEntry entry, float[] vector) {
        repository.saveReady(entry.id(), vector, model.modelName(), entry.contentHash(), clock.instant());
    }

    private static EmbeddingDocument document(StaleEntry entry) {
        return new EmbeddingDocument(entry.title(), entry.content());
    }

    private static String error(RuntimeException e) {
        String text = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }
}
