package com.pastline.ask.dto;

import java.util.List;

/** sources — лише записи, на які посилається перевірена відповідь; removedSentences — скільки речень відкинула перевірка. */
public record AskResponse(
        AskStatus status, String answer, List<AskSource> sources, int removedSentences, long tookMs) {}
