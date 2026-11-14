package com.pastline.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pastline.TestcontainersConfiguration;
import com.pastline.benchmark.BenchmarkReport.QueryResult;
import com.pastline.benchmark.BenchmarkReport.Summary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/** Прогін на мініатюрному корпусі з фейковою моделлю: перевіряє механіку раннера, не якість пошуку. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BenchmarkServiceTest {

    @Autowired
    BenchmarkService benchmarkService;

    @TempDir
    Path dir;

    @Test
    void runProducesReportForAllModes() throws Exception {
        Path queries = Files.createDirectories(dir.resolve("queries"));
        Path corpus = dir.resolve("corpus.json");
        Files.writeString(corpus, """
                [
                  {"id": "c-1", "entryDate": "2026-01-10", "title": null,
                   "content": "Пробіг свій перший напівмарафон, ледве дійшов до фінішу."},
                  {"id": "c-2", "entryDate": "2026-01-11", "title": "Гроші",
                   "content": "Купив трохи ОВДП, дохідність поки виглядає непогано."},
                  {"id": "c-3", "entryDate": "2026-01-12", "title": null,
                   "content": "Зранку був стендап, обговорили блокери по задачі з платежами."},
                  {"id": "c-4", "entryDate": "2026-01-13", "title": null,
                   "content": "Приготував борщ за маминим рецептом. Пробіг свій перший напівмарафон, ледве дійшов до фінішу."}
                ]
                """);
        Files.writeString(queries.resolve("targets.json"), """
                [
                  {"id": "T1", "sentence": "Пробіг свій перший напівмарафон, ледве дійшов до фінішу.", "equivalents": []},
                  {"id": "T2", "sentence": "Купив трохи ОВДП, дохідність поки виглядає непогано.", "equivalents": []},
                  {"id": "T3", "sentence": "Зранку був стендап, обговорили блокери по задачі з платежами.", "equivalents": []}
                ]
                """);
        Files.writeString(queries.resolve("set.json"), """
                [
                  {"type": "exact", "lang": "uk", "text": "напівмарафон", "targets": ["T1"]},
                  {"target": "T2", "lang": "uk", "text": "державні облігації"},
                  {"target": "T3", "lang": "en", "text": "daily standup"}
                ]
                """);

        BenchmarkConfig cfg = new BenchmarkConfig(
                corpus, queries, dir.resolve("results"), "bench-" + UUID.randomUUID() + "@example.com", 1, 2);
        BenchmarkService.Result result = benchmarkService.run(cfg);

        assertThat(result.file()).exists();
        assertThat(Files.readString(result.file())).contains("\"summary\"").contains("\"corpusSha256\"");

        BenchmarkReport report = result.report();
        assertThat(report.completedModes()).containsExactly("lexical", "semantic", "hybrid");
        assertThat(report.config().corpusEntries()).isEqualTo(4);
        assertThat(report.config().queries()).isEqualTo(3);
        assertThat(report.config().embeddingModel()).isEqualTo("fake-bow");
        assertThat(report.summary())
                .filteredOn(s -> s.group().equals("all"))
                .extracting(Summary::mode)
                .containsExactly("lexical", "semantic", "hybrid");
        assertThat(report.summary()).extracting(Summary::group).contains("type:exact", "type:paraphrase", "source:set");

        QueryResult exact = report.queries().getFirst();
        assertThat(exact.relevant()).isEqualTo(2); // c-1 і c-4 містять речення T1
        assertThat(exact.modes().get("lexical").firstRelevantRank()).isEqualTo(1);
        assertThat(exact.modes().get("lexical").recall10()).isEqualTo(1.0);
        assertThat(exact.modes().get("lexical").top()).containsExactlyInAnyOrder("c-1", "c-4");

        QueryResult shortFormat = report.queries().get(1);
        assertThat(shortFormat.id()).isEqualTo("set-02");
        assertThat(shortFormat.type()).isEqualTo("paraphrase");
        assertThat(report.queries().get(2).type()).isEqualTo("crosslingual");
    }

    @Test
    void unknownTargetIsRejectedBeforeRunning() throws Exception {
        Path queries = Files.createDirectories(dir.resolve("bad"));
        Files.writeString(queries.resolve("targets.json"), "[{\"id\": \"T1\", \"sentence\": \"x\"}]");
        Files.writeString(queries.resolve("q.json"), "[{\"target\": \"T999\", \"text\": \"щось\"}]");
        Path corpus = dir.resolve("c.json");
        Files.writeString(corpus, "[]");

        assertThatThrownBy(() -> benchmarkService.run(
                        new BenchmarkConfig(corpus, queries, dir.resolve("r"), "bench-bad@example.com", 0, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("T999");
    }
}
