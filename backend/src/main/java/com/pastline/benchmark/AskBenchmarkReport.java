package com.pastline.benchmark;

import java.util.List;

/** Результат прогону Ask Pastline: JSON у docs/experiments/results, поруч CSV для ручної розмітки. */
public record AskBenchmarkReport(String startedAt, Config config, Summary summary, List<QuestionResult> questions) {

    public record Config(
            String chatModel,
            String embeddingModel,
            int topK,
            double temperature,
            String corpusSha256,
            String questionsSha256,
            int questions) {}

    /**
     * Частки рахуються від кількості питань відповідної групи (answer / refuse).
     *
     * @param rawAnswersWithFabricatedRefs відповіді моделі, де до перевірки був хоча б один вигаданий [E#]
     * @param answersChangedByValidation відповіді, з яких перевірка прибрала хоча б одне речення
     * @param answersBlockedByValidation відповіді, які перевірка заблокувала повністю (NO_EVIDENCE не з волі моделі)
     * @param citedSourcePrecision частка процитованих записів, що справді містять цільове речення
     * @param contextRecall частка питань з відповіддю, де хоча б один релевантний запис потрапив у контекст
     */
    public record Summary(
            int answerable,
            int unanswerable,
            double correctAnswerRate,
            double falseRefusalRate,
            double correctRefusalRate,
            double unwarrantedAnswerRate,
            int rawCitations,
            int rawFabricatedCitations,
            int rawAnswersWithFabricatedRefs,
            int removedSentences,
            int answersChangedByValidation,
            int answersBlockedByValidation,
            double citedSourcePrecision,
            double contextRecall,
            double latencyP50Ms,
            double latencyP95Ms) {}

    /**
     * @param outcome CORRECT_ANSWER, FALSE_REFUSAL, CORRECT_REFUSAL або UNWARRANTED_ANSWER
     * @param sources процитовані записи після перевірки: "E2 c-0123 2025-05-06"
     */
    public record QuestionResult(
            String id,
            String source,
            String expect,
            String question,
            List<String> targets,
            String status,
            String outcome,
            String answer,
            String rawAnswer,
            int contextEntries,
            boolean relevantInContext,
            int rawCitations,
            int rawFabricatedCitations,
            int removedSentences,
            int citedSources,
            int relevantCitedSources,
            List<String> sources,
            long tookMs) {}
}
