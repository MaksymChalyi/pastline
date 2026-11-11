package lab.eval;

import lab.search.Hit;
import lab.search.Lexical;
import lab.search.SearchIndex;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class Report {

    public record RunInfo(String model, int dimension, double firstNorm, long embedMs, int entryCount) {
    }

    private Report() {
    }

    public static String markdown(RunInfo run, List<QueryResult> results) {
        List<QueryResult> answered = results.stream().filter(r -> r.query().hasAnswer()).toList();
        return "# EXP-000: semantic vs lexical\n\n"
                + format("Дата: %s · модель: %s · розмірність: %d · норма першого вектора: %.6f · embedding: %d мс%n%n",
                LocalDate.now(), run.model(), run.dimension(), run.firstNorm(), run.embedMs())
                + format("Записів: %d, запитів: %d (з відповіддю: %d). Hit і MRR рахуються лише по запитах з відповіддю.%n%n",
                run.entryCount(), results.size(), answered.size())
                + "## Підсумок\n\n" + summaryTable(results) + "\n" + metricsTable(results)
                + "\n## Top-5 по кожному запиту (**жирним** — правильний запис)\n\n" + details(results);
    }

    public static String metricsTable(List<QueryResult> results) {
        List<QueryResult> answered = results.stream().filter(r -> r.query().hasAnswer()).toList();
        List<QueryResult> unanswered = results.stream().filter(r -> !r.query().hasAnswer()).toList();
        List<Integer> sem = answered.stream().map(QueryResult::semanticRank).toList();
        List<Integer> lex = answered.stream().map(QueryResult::lexicalRank).toList();

        return "| Метрика | Semantic | Lexical |\n|---|---|---|\n"
                + format("| Hit@1 | %.2f | %.2f |%n", Metrics.hitAt(sem, 1), Metrics.hitAt(lex, 1))
                + format("| Hit@5 | %.2f | %.2f |%n", Metrics.hitAt(sem, 5), Metrics.hitAt(lex, 5))
                + format("| MRR | %.3f | %.3f |%n", Metrics.mrr(sem), Metrics.mrr(lex))
                + format("| Середній cosine top-1 (запити з відповіддю) | %.3f | — |%n", avgTopCosine(answered))
                + format("| Середній cosine top-1 (запити без відповіді) | %.3f | — |%n", avgTopCosine(unanswered));
    }

    private static String summaryTable(List<QueryResult> results) {
        StringBuilder sb = new StringBuilder()
                .append("| Запит | Очікував | Top-1 semantic | Rank sem | Rank lex | Cosine top-1 | Чому |\n")
                .append("|---|---|---|---|---|---|---|\n");
        for (QueryResult r : results) {
            sb.append(format("| %s | %s | %s | %s | %s | %.3f | |%n",
                    r.query().id(), expectedText(r),
                    r.semantic().getFirst().id(),
                    rankText(r, r.semanticRank()), rankText(r, r.lexicalRank()),
                    r.topCosine()));
        }
        return sb.toString();
    }

    private static String details(List<QueryResult> results) {
        StringBuilder sb = new StringBuilder();
        for (QueryResult r : results) {
            sb.append(format("### %s: %s%n%n", r.query().id(), r.query().text()))
                    .append("Очікував: ").append(expectedText(r))
                    .append("  \nСлова запиту для lexical: ").append(Lexical.tokens(r.query().text())).append("\n\n")
                    .append("| # | Semantic | cosine | Lexical | спільних слів |\n|---|---|---|---|---|\n");
            for (int i = 0; i < SearchIndex.TOP_K; i++) {
                sb.append(format("| %d | %s | %s |%n", i + 1,
                        hitCells(r, r.semantic(), i, "%.3f"),
                        hitCells(r, r.lexical(), i, "%.0f")));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Клітинки "id | оцінка" або прочерки, якщо результатів менше за i. */
    private static String hitCells(QueryResult r, List<Hit> hits, int i, String scoreFormat) {
        if (i >= hits.size()) {
            return "— | —";
        }
        Hit hit = hits.get(i);
        String id = r.isExpected(hit.id()) ? "**" + hit.id() + "**" : hit.id();
        return id + " | " + format(scoreFormat, hit.score());
    }

    private static String expectedText(QueryResult r) {
        return r.query().hasAnswer() ? String.join(", ", r.query().expected()) : "—";
    }

    private static String rankText(QueryResult r, int rank) {
        if (!r.query().hasAnswer()) {
            return "—";
        }
        return rank == 0 ? ">5" : String.valueOf(rank);
    }

    private static double avgTopCosine(List<QueryResult> results) {
        return results.stream().mapToDouble(QueryResult::topCosine).average().orElse(Double.NaN);
    }

    private static String format(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }
}
