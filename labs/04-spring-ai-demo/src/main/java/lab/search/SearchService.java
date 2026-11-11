package lab.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final int TOP_K = 5;

    private final EmbeddingModel embeddingModel;
    private final ObjectMapper mapper;
    private final boolean indexOnStartup;

    private List<Entry> entries = List.of();
    private Map<String, float[]> vectors = Map.of();

    public SearchService(EmbeddingModel embeddingModel, ObjectMapper mapper,
                         @Value("${demo.index-on-startup:true}") boolean indexOnStartup) {
        this.embeddingModel = embeddingModel;
        this.mapper = mapper;
        this.indexOnStartup = indexOnStartup;
    }

    /** Вектори 30 записів рахуються один раз після старту; у тестах вимкнено, щоб не ходити в Ollama. */
    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        if (indexOnStartup) {
            ensureIndexed();
        }
    }

    public List<SearchHit> search(String query) {
        ensureIndexed();
        float[] queryVector = embeddingModel.embed(query);
        return entries.stream()
                .map(e -> new SearchHit(e.id(), e.text(), Cosine.similarity(queryVector, vectors.get(e.id()))))
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed().thenComparing(SearchHit::id))
                .limit(TOP_K)
                .toList();
    }

    private synchronized void ensureIndexed() {
        if (!vectors.isEmpty()) {
            return;
        }
        long start = System.nanoTime();
        List<Entry> loaded = readEntries();
        List<float[]> embedded = embeddingModel.embed(loaded.stream().map(Entry::text).toList());

        Map<String, float[]> byId = new LinkedHashMap<>();
        for (int i = 0; i < loaded.size(); i++) {
            byId.put(loaded.get(i).id(), embedded.get(i));
        }
        entries = loaded;
        vectors = byId;
        log.info("Проіндексовано {} записів, розмірність {}, за {} мс",
                loaded.size(), embedded.getFirst().length, (System.nanoTime() - start) / 1_000_000);
    }

    private List<Entry> readEntries() {
        try (InputStream in = new ClassPathResource("entries.json").getInputStream()) {
            return mapper.readValue(in, new TypeReference<>() { });
        } catch (IOException e) {
            throw new UncheckedIOException("Не вдалося прочитати entries.json", e);
        }
    }
}
