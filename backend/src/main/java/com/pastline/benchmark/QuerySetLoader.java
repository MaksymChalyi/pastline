package com.pastline.benchmark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Читає папку набору запитів: targets.json (цільові речення) і всі інші *.json як файли запитів. Ім'я файлу стає
 * джерелом запиту. Підтримує два формати елементів: повний ({type, lang, text, targets}) і короткий ({target, lang,
 * text}); у короткому запит українською вважається paraphrase, іншою мовою - crosslingual.
 */
@Component
@RequiredArgsConstructor
public class QuerySetLoader {

    public static final String TARGETS_FILE = "targets.json";

    private final ObjectMapper objectMapper;

    /** id цілі -> речення, і id цілі -> еквіваленти іншою мовою. */
    public record Targets(Map<String, String> sentences, Map<String, List<String>> equivalents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TargetDto(String id, String sentence, List<String> equivalents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QueryDto(String id, String type, String lang, String text, List<String> targets, String target) {}

    public Targets loadTargets(Path dir) throws IOException {
        List<TargetDto> list =
                objectMapper.readValue(Files.readAllBytes(dir.resolve(TARGETS_FILE)), new TypeReference<>() {});
        Map<String, String> sentences = new LinkedHashMap<>();
        Map<String, List<String>> equivalents = new LinkedHashMap<>();
        for (TargetDto t : list) {
            sentences.put(t.id(), t.sentence());
            equivalents.put(t.id(), t.equivalents() == null ? List.of() : t.equivalents());
        }
        return new Targets(sentences, equivalents);
    }

    public List<BenchmarkQuery> loadQueries(Path dir, Targets targets) throws IOException {
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().equals(TARGETS_FILE))
                    .sorted()
                    .toList();
        }
        List<BenchmarkQuery> queries = new ArrayList<>();
        for (Path file : files) {
            String source = file.getFileName().toString().replaceFirst("\\.json$", "");
            List<QueryDto> items = objectMapper.readValue(Files.readAllBytes(file), new TypeReference<>() {});
            for (int i = 0; i < items.size(); i++) {
                queries.add(toQuery(items.get(i), source, i + 1, targets, file));
            }
        }
        return queries;
    }

    private static BenchmarkQuery toQuery(QueryDto q, String source, int n, Targets targets, Path file) {
        String where = file.getFileName() + " #" + n;
        String lang = q.lang() == null ? "uk" : q.lang();
        String type = q.type() != null ? q.type() : "uk".equals(lang) ? "paraphrase" : "crosslingual";
        String id = q.id() != null ? q.id() : "%s-%02d".formatted(source, n);
        if (q.text() == null || q.text().isBlank()) {
            throw new IllegalArgumentException(where + ": порожній text");
        }

        List<String> declared = new ArrayList<>();
        if (q.targets() != null) {
            declared.addAll(q.targets());
        }
        if (q.target() != null) {
            declared.add(q.target());
        }
        Set<String> expanded = new LinkedHashSet<>();
        for (String t : declared) {
            if (!targets.sentences().containsKey(t)) {
                throw new IllegalArgumentException(where + ": невідома ціль " + t);
            }
            expanded.add(t);
            expanded.addAll(targets.equivalents().getOrDefault(t, List.of()));
        }
        if (expanded.isEmpty()) {
            throw new IllegalArgumentException(where + ": немає targets");
        }
        return new BenchmarkQuery(id, source, type, lang, q.text().strip(), List.copyOf(expanded));
    }
}
