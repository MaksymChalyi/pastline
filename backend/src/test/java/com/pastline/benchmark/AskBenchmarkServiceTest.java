package com.pastline.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pastline.TestcontainersConfiguration;
import com.pastline.benchmark.AskBenchmarkReport.QuestionResult;
import com.pastline.support.FakeChatModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/** Механіка оцінки H5 з фейковою моделлю: підрахунок вигаданих посилань, класифікація результату, CSV. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AskBenchmarkServiceTest {

    @Autowired
    AskBenchmarkService askBenchmarkService;

    @Autowired
    FakeChatModel chat;

    @TempDir
    Path dir;

    Path corpus;
    Path targetsDir;

    @BeforeEach
    void setUp() throws Exception {
        chat.reset();
        targetsDir = Files.createDirectories(dir.resolve("queries"));
        corpus = dir.resolve("corpus.json");
        Files.writeString(corpus, """
                [
                  {"id": "c-1", "entryDate": "2026-01-10", "title": null,
                   "content": "Пробіг свій перший напівмарафон, ледве дійшов до фінішу."},
                  {"id": "c-2", "entryDate": "2026-01-11", "title": "Гроші",
                   "content": "Купив трохи ОВДП, дохідність поки виглядає непогано."}
                ]
                """);
        Files.writeString(targetsDir.resolve("targets.json"), """
                [
                  {"id": "T1", "sentence": "Пробіг свій перший напівмарафон, ледве дійшов до фінішу.", "equivalents": []},
                  {"id": "T2", "sentence": "Купив трохи ОВДП, дохідність поки виглядає непогано.", "equivalents": []}
                ]
                """);
    }

    @Test
    void countsFabricatedCitationsAndClassifiesOutcomes() throws Exception {
        Path questions = dir.resolve("questions.json");
        Files.writeString(questions, """
                [
                  {"id": "q1", "source": "test", "question": "Як пройшов мій перший напівмарафон?",
                   "expect": "answer", "targets": ["T1"]},
                  {"id": "q2", "source": "test", "question": "Як звали мою собаку?", "expect": "refuse"}
                ]
                """);
        // Одна й та сама відповідь на обидва питання: одне справжнє посилання і одне вигадане
        chat.answerWith("Ти пробіг напівмарафон [E1]. А ще купив яхту [E9].");

        AskBenchmarkService.Result result =
                askBenchmarkService.run(corpus, targetsDir, questions, dir.resolve("results"), newUser());

        QuestionResult q1 = result.report().questions().get(0);
        assertThat(q1.outcome()).isEqualTo("CORRECT_ANSWER");
        assertThat(q1.rawCitations()).isEqualTo(2);
        assertThat(q1.rawFabricatedCitations()).isEqualTo(1);
        assertThat(q1.removedSentences()).isEqualTo(1);
        assertThat(q1.relevantInContext()).isTrue();
        assertThat(q1.answer()).doesNotContain("яхту");

        QuestionResult q2 = result.report().questions().get(1);
        assertThat(q2.outcome()).isEqualTo("UNWARRANTED_ANSWER");
        assertThat(q2.relevantCitedSources()).isZero();

        AskBenchmarkReport.Summary s = result.report().summary();
        assertThat(s.answerable()).isEqualTo(1);
        assertThat(s.unanswerable()).isEqualTo(1);
        assertThat(s.correctAnswerRate()).isEqualTo(1.0);
        assertThat(s.unwarrantedAnswerRate()).isEqualTo(1.0);
        assertThat(s.rawFabricatedCitations()).isEqualTo(2);
        assertThat(s.rawAnswersWithFabricatedRefs()).isEqualTo(2);
        assertThat(s.answersChangedByValidation()).isEqualTo(2);
        assertThat(s.answersBlockedByValidation()).isZero();
        assertThat(result.report().config().chatModel()).isEqualTo("fake-chat");

        assertThat(result.json()).exists();
        assertThat(Files.readString(result.csv()))
                .startsWith("﻿id;expect;question")
                .contains("\"q1\";\"answer\"")
                .contains("CORRECT_ANSWER");
    }

    @Test
    void refusalByModelIsACorrectRefusal() throws Exception {
        Path questions = dir.resolve("questions.json");
        Files.writeString(questions, """
                [{"id": "q1", "source": "test", "question": "Як звали мою собаку?", "expect": "refuse"}]
                """);
        chat.answerWith("NO_ANSWER");

        AskBenchmarkReport.Summary s = askBenchmarkService
                .run(corpus, targetsDir, questions, dir.resolve("results"), newUser())
                .report()
                .summary();
        assertThat(s.correctRefusalRate()).isEqualTo(1.0);
        assertThat(s.answersBlockedByValidation()).isZero();
    }

    @Test
    void invalidQuestionSetIsRejectedBeforeRunning() throws Exception {
        Path questions = dir.resolve("bad.json");
        Files.writeString(questions, """
                [{"id": "q1", "source": "test", "question": "Що?", "expect": "answer", "targets": ["T999"]}]
                """);
        assertThatThrownBy(() -> askBenchmarkService.run(corpus, targetsDir, questions, dir, newUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("T999");
        assertThat(chat.userPrompts()).isEmpty();
    }

    private static String newUser() {
        return "ask-bench-" + UUID.randomUUID() + "@example.com";
    }
}
