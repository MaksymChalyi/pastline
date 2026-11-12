package com.pastline.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.UUID;

/**
 * rank починається з 1; score залежить від режиму, тож порівнювати його між режимами не можна.
 *
 * @param debug лише для hybrid з debug=true: звідки взявся результат і як порахований його бал
 */
public record SearchHit(
        UUID id,
        LocalDate entryDate,
        String title,
        String snippet,
        double score,
        int rank,
        @JsonInclude(JsonInclude.Include.NON_NULL) HitDebug debug) {

    public SearchHit(UUID id, LocalDate entryDate, String title, String snippet, double score, int rank) {
        this(id, entryDate, title, snippet, score, rank, null);
    }

    public SearchHit withoutDebug() {
        return debug == null ? this : new SearchHit(id, entryDate, title, snippet, score, rank, null);
    }

    /** Ранги в кожному з режимів (null: запис не потрапив у кандидати цього режиму) і підсумковий RRF-бал. */
    public record HitDebug(Integer lexicalRank, Integer semanticRank, double rrfScore) {}
}
