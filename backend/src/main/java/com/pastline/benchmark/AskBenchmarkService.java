package com.pastline.benchmark;

import com.pastline.ai.port.ChatModelPort;
import com.pastline.ai.port.EmbeddingModelPort;
import com.pastline.ask.dto.AskSource;
import com.pastline.ask.dto.AskStatus;
import com.pastline.ask.service.AskResult;
import com.pastline.ask.service.AskService;
import com.pastline.ask.service.CitationValidator;
import com.pastline.benchmark.AskBenchmarkReport.Config;
import com.pastline.benchmark.AskBenchmarkReport.QuestionResult;
import com.pastline.benchmark.AskBenchmarkReport.Summary;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Оцінка Ask Pastline: кожне питання один раз через AskService; сира відповідь порівнюється з перевіреною.
 * Точність переказу розмічається вручну в CSV.
 */
@Slf4j
@Service
public class AskBenchmarkService {

    public record Result(Path json, Path csv, AskBenchmarkReport report) {}

    private final ObjectMapper objectMapper;
    private final QuerySetLoader querySetLoader;
    private final CorpusPreparer corpusPreparer;
    private final AskService askService;
    private final ChatModelPort chatModel;
    private final EmbeddingModelPort embeddingModel;
    private final Clock clock;
    private final int topK;
    private final double temperature;

    public AskBenchmarkService(
            ObjectMapper objectMapper,
            QuerySetLoader querySetLoader,
            CorpusPreparer corpusPreparer,
            AskService askService,
            ChatModelPort chatModel,
            EmbeddingModelPort embeddingModel,
            Clock clock,
            @Value("${pastline.ask.top-k:8}") int topK,
            @Value("${pastline.ask.temperature:0.2}") double temperature) {
        this.objectMapper = objectMapper;
        this.querySetLoader = querySetLoader;
        this.corpusPreparer = corpusPreparer;
        this.askService = askService;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.clock = clock;
        this.topK = topK;
        this.temperature = temperature;
    }

    public Result run(Path corpus, Path targetsDir, Path questionsFile, Path outputDir, String userEmail)
            throws IOException {
        String startedAt = clock.instant().toString();
        QuerySetLoader.Targets targets = querySetLoader.loadTargets(targetsDir);
        byte[] questionBytes = Files.readAllBytes(questionsFile);
        List<AskQuestion> questions = objectMapper.readValue(questionBytes, new TypeReference<>() {});
        validate(questions, targets);

        CorpusPreparer.Prepared prepared = corpusPreparer.prepare(corpus, userEmail);
        Map<UUID, String> corpusIdByDbId = new HashMap<>();
        prepared.dbIdByCorpusId().forEach((c, db) -> corpusIdByDbId.put(db, c));
        log.info("Ask-бенчмарк: {} питань, модель {}", questions.size(), chatModel.modelName());

        List<QuestionResult> results = new ArrayList<>();
        for (AskQuestion q : questions) {
            Set<UUID> relevant = q.expect().equals(AskQuestion.ANSWER)
                    ? prepared.entriesContaining(sentences(q.targets(), targets))
                    : Set.of();
            if (q.expect().equals(AskQuestion.ANSWER) && relevant.isEmpty()) {
                throw new IllegalStateException("Питання " + q.id() + ": у корпусі немає релевантного запису");
            }
            AskResult r = askService.ask(prepared.userId(), q.question());
            results.add(evaluate(q, r, relevant, corpusIdByDbId));
            log.info(
                    "{}: {} ({} мс)",
                    q.id(),
                    results.getLast().outcome(),
                    r.response().tookMs());
        }

        Config config = new Config(
                chatModel.modelName(),
                embeddingModel.modelName(),
                topK,
                temperature,
                prepared.corpusSha256(),
                CorpusPreparer.sha256(questionBytes),
                questions.size());
        AskBenchmarkReport report = new AskBenchmarkReport(startedAt, config, summarize(results), results);

        Files.createDirectories(outputDir);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now(clock));
        Path json = outputDir.resolve("run-ask-" + stamp + ".json");
        Path csv = outputDir.resolve("run-ask-" + stamp + ".csv");
        Files.writeString(json, objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(report));
        Files.writeString(csv, toCsv(results), StandardCharsets.UTF_8);
        log.info("Підсумок: {}", report.summary());
        log.info("Звіт: {}, таблиця для розмітки: {}", json.toAbsolutePath(), csv.toAbsolutePath());
        return new Result(json, csv, report);
    }

    static QuestionResult evaluate(AskQuestion q, AskResult r, Set<UUID> relevant, Map<UUID, String> corpusIdByDbId) {
        List<UUID> context = r.contextIds();
        Set<String> allowed =
                IntStream.rangeClosed(1, context.size()).mapToObj(i -> "E" + i).collect(Collectors.toSet());
        List<String> rawRefs = CitationValidator.refsIn(r.rawAnswer());
        int fabricated =
                (int) rawRefs.stream().filter(ref -> !allowed.contains(ref)).count();

        boolean answered = r.response().status() == AskStatus.ANSWERED;
        boolean shouldAnswer = q.expect().equals(AskQuestion.ANSWER);
        String outcome = shouldAnswer
                ? (answered ? "CORRECT_ANSWER" : "FALSE_REFUSAL")
                : (answered ? "UNWARRANTED_ANSWER" : "CORRECT_REFUSAL");

        List<AskSource> sources = r.response().sources();
        int relevantCited =
                (int) sources.stream().filter(s -> relevant.contains(s.id())).count();
        return new QuestionResult(
                q.id(),
                q.source(),
                q.expect(),
                q.question(),
                q.targets() == null ? List.of() : q.targets(),
                r.response().status().name(),
                outcome,
                r.response().answer(),
                r.rawAnswer(),
                context.size(),
                context.stream().anyMatch(relevant::contains),
                rawRefs.size(),
                fabricated,
                r.response().removedSentences(),
                sources.size(),
                relevantCited,
                sources.stream()
                        .map(s -> s.ref() + " " + corpusIdByDbId.getOrDefault(s.id(), "?") + " " + s.entryDate())
                        .toList(),
                r.response().tookMs());
    }

    static Summary summarize(List<QuestionResult> results) {
        List<QuestionResult> answerable = results.stream()
                .filter(r -> r.expect().equals(AskQuestion.ANSWER))
                .toList();
        List<QuestionResult> unanswerable = results.stream()
                .filter(r -> r.expect().equals(AskQuestion.REFUSE))
                .toList();
        int cited = results.stream().mapToInt(QuestionResult::citedSources).sum();
        int relevantCited =
                results.stream().mapToInt(QuestionResult::relevantCitedSources).sum();
        List<Double> times = results.stream().map(r -> (double) r.tookMs()).toList();
        return new Summary(
                answerable.size(),
                unanswerable.size(),
                share(answerable, "CORRECT_ANSWER"),
                share(answerable, "FALSE_REFUSAL"),
                share(unanswerable, "CORRECT_REFUSAL"),
                share(unanswerable, "UNWARRANTED_ANSWER"),
                results.stream().mapToInt(QuestionResult::rawCitations).sum(),
                results.stream()
                        .mapToInt(QuestionResult::rawFabricatedCitations)
                        .sum(),
                (int) results.stream()
                        .filter(r -> r.rawFabricatedCitations() > 0)
                        .count(),
                results.stream().mapToInt(QuestionResult::removedSentences).sum(),
                (int) results.stream().filter(r -> r.removedSentences() > 0).count(),
                (int) results.stream()
                        .filter(r -> r.status().equals(AskStatus.NO_EVIDENCE.name()) && r.removedSentences() > 0)
                        .count(),
                cited == 0 ? 0 : round((double) relevantCited / cited),
                answerable.isEmpty()
                        ? 0
                        : round((double) answerable.stream()
                                        .filter(QuestionResult::relevantInContext)
                                        .count()
                                / answerable.size()),
                BenchmarkMetrics.percentile(times, 0.5),
                BenchmarkMetrics.percentile(times, 0.95));
    }

    /** Розділювач ";" і BOM: Excel з українською локаллю відкриває файл без майстра імпорту. */
    static String toCsv(List<QuestionResult> results) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("id;expect;question;status;outcome;answer;sources;raw_answer;accurate (так/ні);notes\n");
        for (QuestionResult r : results) {
            sb.append(String.join(
                            ";",
                            csv(r.id()),
                            csv(r.expect()),
                            csv(r.question()),
                            csv(r.status()),
                            csv(r.outcome()),
                            csv(r.answer()),
                            csv(String.join(", ", r.sources())),
                            csv(r.rawAnswer()),
                            "",
                            ""))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        String v = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static void validate(List<AskQuestion> questions, QuerySetLoader.Targets targets) {
        Set<String> ids = new HashSet<>();
        for (AskQuestion q : questions) {
            if (q.id() == null || !ids.add(q.id())) {
                throw new IllegalArgumentException("Порожній або повторний id питання: " + q.id());
            }
            if (q.question() == null || q.question().isBlank()) {
                throw new IllegalArgumentException(q.id() + ": порожнє питання");
            }
            if (!AskQuestion.ANSWER.equals(q.expect()) && !AskQuestion.REFUSE.equals(q.expect())) {
                throw new IllegalArgumentException(q.id() + ": expect має бути answer або refuse");
            }
            List<String> t = q.targets() == null ? List.of() : q.targets();
            if (AskQuestion.ANSWER.equals(q.expect()) && t.isEmpty()) {
                throw new IllegalArgumentException(q.id() + ": для answer потрібні targets");
            }
            for (String target : t) {
                if (!targets.sentences().containsKey(target)) {
                    throw new IllegalArgumentException(q.id() + ": невідома ціль " + target);
                }
            }
        }
    }

    private static List<String> sentences(List<String> ids, QuerySetLoader.Targets targets) {
        Set<String> all = new HashSet<>(ids);
        ids.forEach(id -> all.addAll(targets.equivalents().getOrDefault(id, List.of())));
        return all.stream().map(targets.sentences()::get).toList();
    }

    private static double share(List<QuestionResult> group, String outcome) {
        return group.isEmpty()
                ? 0
                : round((double) group.stream()
                                .filter(r -> r.outcome().equals(outcome))
                                .count()
                        / group.size());
    }

    private static double round(double value) {
        return Math.round(value * 10_000) / 10_000.0;
    }
}
