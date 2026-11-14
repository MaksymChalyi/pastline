package com.pastline.ask.dto;

import java.util.List;

/**
 * sources: лише записи, на які посилається відповідь після перевірки. removedSentences: скільки речень відкинула
 * перевірка (без джерела або з вигаданим), для вимірювання H5.
 */
public record AskResponse(
        AskStatus status, String answer, List<AskSource> sources, int removedSentences, long tookMs) {}
