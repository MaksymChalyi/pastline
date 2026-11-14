package com.pastline.benchmark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pastline.ai.port.EmbeddingModelPort;
import com.pastline.auth.service.AuthService;
import com.pastline.benchmark.BenchmarkReport.Config;
import com.pastline.benchmark.BenchmarkReport.ModeResult;
import com.pastline.benchmark.BenchmarkReport.QueryResult;
import com.pastline.benchmark.BenchmarkReport.Summary;
import com.pastline.importing.dto.ImportReport;
import com.pastline.importing.service.ImportService;
import com.pastline.journal.service.JournalService;
import com.pastline.search.dto.SearchHit;
import com.pastline.search.embedding.EmbeddingIndexer;
import com.pastline.search.repository.EmbeddingRepository;
import com.pastline.search.service.SearchMode;
import com.pastline.search.service.SearchService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Відтворюваний прогін трьох режимів пошуку на корпусі і наборі запитів.
 *
 * <ol>
 *   <li>окремий користувач, імпорт корпусу (повторний запуск нічого не дублює), очікування всіх векторів;
 *   <li>еталон: записи корпусу, що містять цільові речення запиту (з еквівалентами іншою мовою);
 *   <li>для кожного режиму: прогрів, потім кожен запит repeats разів; метрики з першого виміряного прогону, затримка з
 *       усіх;
 *   <li>JSON-звіт з конфігурацією і хешами вхідних файлів, щоб прогін можна було повторити.
 * </ol>
 *
 * Пошук викликається напряму через SearchService: затримка без HTTP і cookie, але з обчисленням вектора запиту.
 */
@Slf4j
@Service
public class BenchmarkService {

    private static final List<SearchMode> MODES = List.of(SearchMode.LEXICAL, SearchMode.SEMANTIC, SearchMode.HYBRID);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CorpusEntry(String id, String entryDate, String title, String content) {}

    private final ObjectMapper objectMapper;
    private final QuerySetLoader querySetLoader;
    private final AuthService authService;
    private final ImportService importService;
    private final EmbeddingIndexer indexer;
    private final EmbeddingRepository embeddingRepository;
    private final EmbeddingModelPort embeddingModel;
    private final SearchService searchService;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final int rrfK;

    public BenchmarkService(
            ObjectMapper objectMapper,
            QuerySetLoader querySetLoader,
            AuthService authService,
            ImportService importService,
            EmbeddingIndexer indexer,
            EmbeddingRepository embeddingRepository,
            EmbeddingModelPort embeddingModel,
            SearchService searchService,
            JdbcTemplate jdbc,
            Clock clock,
            @Value("${pastline.search.rrf-k:60}") int rrfK) {
        this.objectMapper = objectMapper;
        this.querySetLoader = querySetLoader;
        this.authService = authService;
        this.importService = importService;
        this.indexer = indexer;
        this.embeddingRepository = embeddingRepository;
        this.embeddingModel = embeddingModel;
        this.searchService = searchService;
        this.jdbc = jdbc;
        this.clock = clock;
        this.rrfK = rrfK;
    }

    public record Result(Path file, BenchmarkReport report) {}

    public Result run(BenchmarkConfig cfg) throws IOException {
        String startedAt = clock.instant().toString();
        byte[] corpusBytes = Files.readAllBytes(cfg.corpus());
        List<CorpusEntry> corpus = objectMapper.readValue(corpusBytes, new TypeReference<>() {});
        QuerySetLoader.Targets targets = querySetLoader.loadTargets(cfg.queriesDir());
        List<BenchmarkQuery> queries = querySetLoader.loadQueries(cfg.queriesDir(), targets);
        log.info("Корпус: {} записів, запитів: {}", corpus.size(), queries.size());

        UUID userId = ensureUser(cfg.userEmail());
        ImportReport imported = importService.importFile(userId, "corpus.json", corpusBytes, false);
        log.info(
                "Імпорт: створено {}, вже було {}, помилок {}",
                imported.created(),
                imported.duplicates(),
                imported.failed());
        if (imported.failed() > 0) {
            throw new IllegalStateException("Корпус імпортовано з помилками: " + imported.errors());
        }

        log.info("Індексація векторів...");
        indexer.indexAll();
        int stale = embeddingRepository.countStale(userId, embeddingModel.modelName());
        if (stale > 0) {
            throw new IllegalStateException(
                    stale + " записів без вектора: модель ембедінгів недоступна або індексація неповна");
        }
        int failed = countFailedEmbeddings(userId);

        Map<String, UUID> dbIdByCorpusId = mapCorpusToDb(userId, corpus);
        Map<UUID, String> corpusIdByDbId = new HashMap<>();
        dbIdByCorpusId.forEach((c, db) -> corpusIdByDbId.put(db, c));

        Map<String, Set<UUID>> relevantByQuery = new HashMap<>();
        for (BenchmarkQuery q : queries) {
            relevantByQuery.put(q.id(), relevant(q, targets, corpus, dbIdByCorpusId));
        }

        Config config = new Config(
                embeddingModel.modelName(),
                rrfK,
                BenchmarkMetrics.DEPTH,
                cfg.warmup(),
                cfg.repeats(),
                sha256(corpusBytes),
                corpus.size(),
                queryFileHashes(cfg.queriesDir()),
                queries.size(),
                failed,
                System.getProperty("java.version"));
        Files.createDirectories(cfg.outputDir());
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now(clock));
        Path file = cfg.outputDir().resolve("run-" + stamp + ".json");

        Map<String, Map<String, ModeResult>> resultsByQuery = new LinkedHashMap<>();
        Map<String, Map<String, List<Double>>> latenciesByQuery = new HashMap<>();
        List<String> completedModes = new ArrayList<>();
        BenchmarkReport report = null;
        for (SearchMode mode : MODES) {
            log.info("Режим {}: прогрів {}, повторів {}", mode.apiName(), cfg.warmup(), cfg.repeats());
            for (int i = 0; i < Math.min(cfg.warmup(), queries.size()); i++) {
                search(userId, queries.get(i).text(), mode);
            }
            for (BenchmarkQuery q : queries) {
                List<UUID> ranked = null;
                List<Double> times = new ArrayList<>();
                for (int r = 0; r < Math.max(1, cfg.repeats()); r++) {
                    long start = System.nanoTime();
                    List<UUID> ids = search(userId, q.text(), mode);
                    times.add((System.nanoTime() - start) / 1_000_000.0);
                    if (ranked == null) {
                        ranked = ids;
                    }
                }
                Set<UUID> relevant = relevantByQuery.get(q.id());
                ModeResult result = new ModeResult(
                        BenchmarkMetrics.firstRelevantRank(ranked, relevant),
                        round(BenchmarkMetrics.recall(ranked, relevant)),
                        round(BenchmarkMetrics.percentile(times, 0.5)),
                        ranked.stream()
                                .map(id -> corpusIdByDbId.getOrDefault(id, "?"))
                                .toList());
                resultsByQuery
                        .computeIfAbsent(q.id(), k -> new LinkedHashMap<>())
                        .put(mode.apiName(), result);
                latenciesByQuery.computeIfAbsent(q.id(), k -> new HashMap<>()).put(mode.apiName(), times);
            }
            completedModes.add(mode.apiName());
            // Після кожного режиму звіт перезаписується: обрив (сон ноутбука, падіння Ollama) не губить готове
            report = buildReport(
                    startedAt, config, queries, relevantByQuery, completedModes, resultsByQuery, latenciesByQuery);
            Files.writeString(
                    file, objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(report));
            log.info("Режим {} готовий, проміжний звіт: {}", mode.apiName(), file.toAbsolutePath());
        }
        logSummary(report.summary());
        log.info("Звіт: {}", file.toAbsolutePath());
        return new Result(file, report);
    }

    private static BenchmarkReport buildReport(
            String startedAt,
            Config config,
            List<BenchmarkQuery> queries,
            Map<String, Set<UUID>> relevantByQuery,
            List<String> completedModes,
            Map<String, Map<String, ModeResult>> resultsByQuery,
            Map<String, Map<String, List<Double>>> latenciesByQuery) {
        List<QueryResult> queryResults = queries.stream()
                .map(q -> new QueryResult(
                        q.id(),
                        q.source(),
                        q.type(),
                        q.lang(),
                        q.text(),
                        q.targets(),
                        relevantByQuery.get(q.id()).size(),
                        resultsByQuery.get(q.id())))
                .toList();
        List<Summary> summary = new ArrayList<>();
        for (String m : completedModes) {
            summary.add(summarize(m, "all", queries, q -> true, resultsByQuery, latenciesByQuery));
            for (String type : distinct(queries.stream().map(BenchmarkQuery::type))) {
                summary.add(summarize(
                        m, "type:" + type, queries, q -> q.type().equals(type), resultsByQuery, latenciesByQuery));
            }
            for (String source : distinct(queries.stream().map(BenchmarkQuery::source))) {
                summary.add(summarize(
                        m,
                        "source:" + source,
                        queries,
                        q -> q.source().equals(source),
                        resultsByQuery,
                        latenciesByQuery));
            }
        }
        return new BenchmarkReport(startedAt, config, List.copyOf(completedModes), summary, queryResults);
    }

    private UUID ensureUser(String email) {
        try {
            return authService.loadUserByUsername(email).id();
        } catch (UsernameNotFoundException e) {
            return authService
                    .register(email, UUID.randomUUID().toString(), null)
                    .getId();
        }
    }

    private int countFailedEmbeddings(UUID userId) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM entry_embeddings x JOIN journal_entries e ON e.id = x.entry_id
                WHERE e.user_id = ? AND x.status = 'FAILED'
                """, Integer.class, userId);
        return n == null ? 0 : n;
    }

    /** Запис корпусу -> запис у БД за ключем дедуплікації (дата + хеш нормалізованого тексту). */
    private Map<String, UUID> mapCorpusToDb(UUID userId, List<CorpusEntry> corpus) {
        Map<String, UUID> dbByKey = new HashMap<>();
        jdbc.query(
                "SELECT id, entry_date, content_hash FROM journal_entries WHERE user_id = ?",
                rs -> {
                    dbByKey.put(
                            JournalService.key(
                                    rs.getObject("entry_date", LocalDate.class), rs.getString("content_hash")),
                            rs.getObject("id", UUID.class));
                },
                userId);
        Map<String, UUID> result = new HashMap<>();
        for (CorpusEntry e : corpus) {
            String key = JournalService.key(LocalDate.parse(e.entryDate()), JournalService.contentHash(e.content()));
            UUID id = dbByKey.get(key);
            if (id == null) {
                throw new IllegalStateException("Запис корпусу " + e.id() + " не знайдено в БД");
            }
            result.put(e.id(), id);
        }
        return result;
    }

    private static Set<UUID> relevant(
            BenchmarkQuery q, QuerySetLoader.Targets targets, List<CorpusEntry> corpus, Map<String, UUID> dbIds) {
        List<String> sentences =
                q.targets().stream().map(t -> targets.sentences().get(t)).toList();
        Set<UUID> relevant = new HashSet<>();
        for (CorpusEntry e : corpus) {
            if (sentences.stream().anyMatch(e.content()::contains)) {
                relevant.add(dbIds.get(e.id()));
            }
        }
        if (relevant.isEmpty()) {
            throw new IllegalStateException("Запит " + q.id() + ": у корпусі немає жодного релевантного запису");
        }
        return relevant;
    }

    private List<UUID> search(UUID userId, String text, SearchMode mode) {
        return searchService.search(userId, text, mode, BenchmarkMetrics.DEPTH, false).hits().stream()
                .map(SearchHit::id)
                .toList();
    }

    private static Summary summarize(
            String mode,
            String group,
            List<BenchmarkQuery> queries,
            Predicate<BenchmarkQuery> filter,
            Map<String, Map<String, ModeResult>> results,
            Map<String, Map<String, List<Double>>> latencies) {
        List<BenchmarkQuery> selected = queries.stream().filter(filter).toList();
        int n = selected.size();
        double hit1 = 0, hit5 = 0, hit10 = 0, mrr = 0, recall = 0;
        List<Double> times = new ArrayList<>();
        for (BenchmarkQuery q : selected) {
            ModeResult r = results.get(q.id()).get(mode);
            int rank = r.firstRelevantRank();
            hit1 += BenchmarkMetrics.hitAt(1, rank) ? 1 : 0;
            hit5 += BenchmarkMetrics.hitAt(5, rank) ? 1 : 0;
            hit10 += BenchmarkMetrics.hitAt(10, rank) ? 1 : 0;
            mrr += BenchmarkMetrics.reciprocalRank(rank);
            recall += r.recall10();
            times.addAll(latencies.get(q.id()).get(mode));
        }
        return new Summary(
                mode,
                group,
                n,
                round(hit1 / n),
                round(hit5 / n),
                round(hit10 / n),
                round(mrr / n),
                round(recall / n),
                round(BenchmarkMetrics.percentile(times, 0.5)),
                round(BenchmarkMetrics.percentile(times, 0.95)));
    }

    private static void logSummary(List<Summary> summary) {
        log.info(String.format(
                "%-9s %-22s %4s %6s %6s %6s %6s %6s %8s %8s",
                "mode", "group", "n", "hit@1", "hit@5", "hit@10", "mrr", "recall", "p50ms", "p95ms"));
        for (Summary s : summary) {
            log.info(String.format(
                    "%-9s %-22s %4d %6.3f %6.3f %6.3f %6.3f %6.3f %8.2f %8.2f",
                    s.mode(),
                    s.group(),
                    s.queries(),
                    s.hit1(),
                    s.hit5(),
                    s.hit10(),
                    s.mrr10(),
                    s.recall10(),
                    s.latencyP50Ms(),
                    s.latencyP95Ms()));
        }
    }

    private static List<String> distinct(Stream<String> values) {
        return values.distinct().sorted().toList();
    }

    private static Map<String, String> queryFileHashes(Path dir) throws IOException {
        Map<String, String> hashes = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f :
                    files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                hashes.put(f.getFileName().toString(), sha256(Files.readAllBytes(f)));
            }
        }
        return hashes;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static double round(double value) {
        return Math.round(value * 10_000) / 10_000.0;
    }
}
