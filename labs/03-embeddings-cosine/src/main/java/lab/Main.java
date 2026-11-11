package lab;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lab.data.Entry;
import lab.data.Query;
import lab.eval.QueryResult;
import lab.eval.Report;
import lab.ollama.EmbeddingClient;
import lab.ollama.OllamaException;
import lab.search.Cosine;
import lab.search.SearchIndex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** EXP-000: semantic (embeddinggemma + cosine) проти lexical (спільні слова). */
public class Main {

    public static void main(String[] args) throws IOException {
        String model = env("EMBED_MODEL", "embeddinggemma");
        EmbeddingClient client = new EmbeddingClient(env("OLLAMA_URL", "http://localhost:11434"), model, Duration.ofMinutes(5));

        ObjectMapper mapper = new ObjectMapper();
        List<Entry> entries = readResource(mapper, "/entries.json", new TypeReference<>() { });
        List<Query> queries = readResource(mapper, "/queries.json", new TypeReference<>() { });

        long start = System.nanoTime();
        List<float[]> entryVectors;
        List<float[]> queryVectors;
        try {
            entryVectors = client.embed(entries.stream().map(Entry::text).toList());
            queryVectors = client.embed(queries.stream().map(Query::text).toList());
        } catch (OllamaException e) {
            System.err.println("Помилка: " + e.getMessage());
            System.exit(1);
            return;
        }
        long embedMs = (System.nanoTime() - start) / 1_000_000;

        // Норма ≈ 1 означає, що модель повертає нормалізовані вектори.
        float[] first = entryVectors.getFirst();
        Report.RunInfo run = new Report.RunInfo(model, first.length, Cosine.norm(first), embedMs, entries.size());
        System.out.printf(Locale.ROOT, "Модель: %s, розмірність: %d, норма першого вектора: %.6f, embedding: %d мс%n%n",
                run.model(), run.dimension(), run.firstNorm(), run.embedMs());

        SearchIndex index = new SearchIndex(entries, entryVectors);
        List<QueryResult> results = new ArrayList<>();
        for (int i = 0; i < queries.size(); i++) {
            Query query = queries.get(i);
            results.add(new QueryResult(query, index.semantic(queryVectors.get(i)), index.lexical(query.text())));
        }

        Path out = Path.of("results-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".md");
        Files.writeString(out, Report.markdown(run, results));
        System.out.println(Report.metricsTable(results));
        System.out.println("Звіт: " + out.toAbsolutePath());
    }

    /** З classpath, тому запуск не залежить від робочої папки. */
    private static <T> T readResource(ObjectMapper mapper, String name, TypeReference<T> type) throws IOException {
        try (InputStream in = Main.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("Не знайдено " + name + " у src/main/resources");
            }
            return mapper.readValue(in, type);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
