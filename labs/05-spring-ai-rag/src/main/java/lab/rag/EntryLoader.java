package lab.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/** Якщо таблиця вже заповнена, повторно не завантажує. */
@Component
public class EntryLoader {

    private static final Logger log = LoggerFactory.getLogger(EntryLoader.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final boolean loadOnStartup;

    public EntryLoader(VectorStore vectorStore, JdbcTemplate jdbc, ObjectMapper mapper,
                       @Value("${demo.load-on-startup:true}") boolean loadOnStartup) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.loadOnStartup = loadOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        if (!loadOnStartup) {
            return;
        }
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
        if (existing != null && existing > 0) {
            log.info("vector_store вже містить {} документів, завантаження пропущено", existing);
            return;
        }

        long start = System.nanoTime();
        // id документа в pgvector — UUID, тому id запису (E07) і дата йдуть у metadata.
        List<Document> documents = readEntries().stream()
                .map(e -> new Document(e.text(), Map.of("entryId", e.id(), "date", e.date())))
                .toList();
        vectorStore.add(documents);
        log.info("Завантажено {} документів у pgvector за {} мс", documents.size(), (System.nanoTime() - start) / 1_000_000);
    }

    private List<Entry> readEntries() {
        try (InputStream in = new ClassPathResource("entries.json").getInputStream()) {
            return mapper.readValue(in, new TypeReference<>() { });
        } catch (IOException e) {
            throw new UncheckedIOException("Не вдалося прочитати entries.json", e);
        }
    }
}
