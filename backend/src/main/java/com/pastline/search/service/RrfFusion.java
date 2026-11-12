package com.pastline.search.service;

import com.pastline.search.dto.SearchHit;
import com.pastline.search.dto.SearchHit.HitDebug;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reciprocal Rank Fusion: score(d) = Σ 1 / (k + rank_i(d)) по всіх списках, де d присутній.
 *
 * <p>Працює лише з рангами, тому бали режимів (ts_rank ~0.06, косинус ~0.4) не треба нормалізувати. Запис, високо
 * присутній в обох списках, піднімається нагору; запис лише з одного списку теж лишається у видачі. Чиста функція без
 * Spring і БД: перевіряється юніт-тестами на ручних прикладах.
 */
public final class RrfFusion {

    /** При рівному балі вище той, хто має кращий ранг хоча б в одному списку; далі дата і id для детермінізму. */
    private static final Comparator<Candidate> ORDER = Comparator.comparingDouble(Candidate::score)
            .reversed()
            .thenComparingInt(Candidate::bestRank)
            .thenComparing((Candidate c) -> c.hit().entryDate(), Comparator.reverseOrder())
            .thenComparing(c -> c.hit().id());

    private RrfFusion() {}

    public static List<SearchHit> fuse(List<SearchHit> lexical, List<SearchHit> semantic, int k, int limit) {
        if (k < 1) {
            throw new IllegalArgumentException("k має бути >= 1");
        }
        Map<UUID, Candidate> byId = new LinkedHashMap<>();
        for (SearchHit hit : lexical) {
            byId.computeIfAbsent(hit.id(), id -> new Candidate(hit)).lexicalRank = hit.rank();
        }
        for (SearchHit hit : semantic) {
            byId.computeIfAbsent(hit.id(), id -> new Candidate(hit)).semanticRank = hit.rank();
        }

        List<Candidate> candidates = new ArrayList<>(byId.values());
        candidates.forEach(c -> c.computeScore(k));
        candidates.sort(ORDER);

        List<SearchHit> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, candidates.size()); i++) {
            Candidate c = candidates.get(i);
            SearchHit h = c.hit();
            result.add(new SearchHit(
                    h.id(),
                    h.entryDate(),
                    h.title(),
                    h.snippet(),
                    c.score(),
                    i + 1,
                    new HitDebug(c.lexicalRank, c.semanticRank, c.score())));
        }
        return result;
    }

    private static final class Candidate {
        private final SearchHit hit;
        private Integer lexicalRank;
        private Integer semanticRank;
        private double score;

        Candidate(SearchHit hit) {
            this.hit = hit;
        }

        void computeScore(int k) {
            score = (lexicalRank == null ? 0 : 1.0 / (k + lexicalRank))
                    + (semanticRank == null ? 0 : 1.0 / (k + semanticRank));
        }

        SearchHit hit() {
            return hit;
        }

        double score() {
            return score;
        }

        int bestRank() {
            return Math.min(
                    lexicalRank == null ? Integer.MAX_VALUE : lexicalRank,
                    semanticRank == null ? Integer.MAX_VALUE : semanticRank);
        }
    }
}
