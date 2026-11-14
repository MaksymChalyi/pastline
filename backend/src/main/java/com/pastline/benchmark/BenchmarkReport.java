package com.pastline.benchmark;

import java.util.List;
import java.util.Map;

/** Результат прогону: зберігається JSON-файлом у docs/experiments/results. */
/** completedModes: режими, що встигли пройти; звіт перезаписується після кожного. */
public record BenchmarkReport(
        String startedAt,
        Config config,
        List<String> completedModes,
        List<Summary> summary,
        List<QueryResult> queries) {

    public record Config(
            String embeddingModel,
            int rrfK,
            String lexicalOperator,
            int depth,
            int warmup,
            int repeats,
            String corpusSha256,
            int corpusEntries,
            Map<String, String> queryFilesSha256,
            int queries,
            int failedEmbeddings,
            String javaVersion) {}

    /** group: "all", "type:paraphrase", "source:claude" тощо. Метрики усереднені по запитах групи. */
    public record Summary(
            String mode,
            String group,
            int queries,
            double hit1,
            double hit5,
            double hit10,
            double mrr10,
            double recall10,
            double latencyP50Ms,
            double latencyP95Ms) {}

    public record QueryResult(
            String id,
            String source,
            String type,
            String lang,
            String text,
            List<String> targets,
            int relevant,
            Map<String, ModeResult> modes) {}

    /**
     * @param firstRelevantRank 1..10, або 0, якщо в top-10 немає релевантних
     * @param top id записів корпусу (c-0001...) у видачі: для аналізу помилок
     */
    public record ModeResult(int firstRelevantRank, double recall10, double latencyMedianMs, List<String> top) {}
}
