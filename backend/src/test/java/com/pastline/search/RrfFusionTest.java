package com.pastline.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.pastline.search.dto.SearchHit;
import com.pastline.search.service.RrfFusion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RrfFusionTest {

    private static final SearchHit A = hit("A");
    private static final SearchHit B = hit("B");
    private static final SearchHit C = hit("C");

    @Test
    void entryFoundByBothModesRanksFirst() {
        // lexical: A, B; semantic: C, A
        List<SearchHit> fused = RrfFusion.fuse(ranked(A, B), ranked(C, A), 60, 10);

        assertThat(fused).extracting(SearchHit::title).containsExactly("A", "C", "B");
        assertThat(fused.get(0).score()).isCloseTo(1.0 / 61 + 1.0 / 62, within(1e-12));
        assertThat(fused.get(1).score()).isCloseTo(1.0 / 61, within(1e-12));
        assertThat(fused.get(2).score()).isCloseTo(1.0 / 62, within(1e-12));
        assertThat(fused).extracting(SearchHit::rank).containsExactly(1, 2, 3);
    }

    @Test
    void debugShowsRanksFromEachMode() {
        List<SearchHit> fused = RrfFusion.fuse(ranked(A, B), ranked(C, A), 60, 10);

        SearchHit.HitDebug a = fused.get(0).debug();
        assertThat(a.lexicalRank()).isEqualTo(1);
        assertThat(a.semanticRank()).isEqualTo(2);
        assertThat(a.rrfScore()).isEqualTo(fused.get(0).score());

        SearchHit.HitDebug b = fused.get(2).debug();
        assertThat(b.lexicalRank()).isEqualTo(2);
        assertThat(b.semanticRank()).isNull();
    }

    @Test
    void kControlsHowMuchTopRanksDominate() {
        // X лише на 1-му місці лексичного; Y на 4-му місці в обох списках
        SearchHit x = hit("X");
        SearchHit y = hit("Y");
        List<SearchHit> lexical = ranked(x, hit("l2"), hit("l3"), y);
        List<SearchHit> semantic = ranked(hit("s1"), hit("s2"), hit("s3"), y);

        // k=1: X = 1/2 = 0.5, Y = 2/5 = 0.4 -> перше місце важить більше за згоду режимів
        assertThat(positionOf("X", RrfFusion.fuse(lexical, semantic, 1, 10)))
                .isLessThan(positionOf("Y", RrfFusion.fuse(lexical, semantic, 1, 10)));
        // k=60: X = 1/61 ≈ 0.0164, Y = 2/64 ≈ 0.0313 -> згода двох режимів важить більше
        assertThat(positionOf("Y", RrfFusion.fuse(lexical, semantic, 60, 10)))
                .isLessThan(positionOf("X", RrfFusion.fuse(lexical, semantic, 60, 10)));
    }

    @Test
    void equalScoresAreOrderedByBestRank() {
        // B: 2-ге місце лише в lexical; C: 2-ге місце лише в semantic -> рівні бали
        // A: 1-ше місце лише в lexical; D: 1-ше лише в semantic -> рівні бали, вищі за B і C
        SearchHit d = hit("D");
        List<SearchHit> fused = RrfFusion.fuse(ranked(A, B), ranked(d, C), 60, 10);

        assertThat(fused.get(0).score()).isEqualTo(fused.get(1).score());
        assertThat(fused.subList(0, 2)).extracting(SearchHit::title).containsExactlyInAnyOrder("A", "D");
        assertThat(fused.subList(2, 4)).extracting(SearchHit::title).containsExactlyInAnyOrder("B", "C");
    }

    @Test
    void limitCutsResultAndRanksStayContinuous() {
        List<SearchHit> fused = RrfFusion.fuse(ranked(A, B), ranked(C), 60, 2);
        assertThat(fused).hasSize(2).extracting(SearchHit::rank).containsExactly(1, 2);
    }

    @Test
    void oneEmptyListGivesOtherListOrder() {
        assertThat(RrfFusion.fuse(List.of(), ranked(C, A, B), 60, 10))
                .extracting(SearchHit::title)
                .containsExactly("C", "A", "B");
        assertThat(RrfFusion.fuse(List.of(), List.of(), 60, 10)).isEmpty();
    }

    @Test
    void nonPositiveKIsRejected() {
        assertThatThrownBy(() -> RrfFusion.fuse(List.of(), List.of(), 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static int positionOf(String title, List<SearchHit> hits) {
        for (int i = 0; i < hits.size(); i++) {
            if (hits.get(i).title().equals(title)) {
                return i;
            }
        }
        throw new AssertionError(title + " відсутній у результаті");
    }

    private static List<SearchHit> ranked(SearchHit... hits) {
        List<SearchHit> result = new ArrayList<>();
        for (int i = 0; i < hits.length; i++) {
            SearchHit h = hits[i];
            result.add(new SearchHit(h.id(), h.entryDate(), h.title(), h.snippet(), 0.5, i + 1));
        }
        return result;
    }

    /** title = мітка, щоб перевіряти порядок читабельно; id детермінований від мітки. */
    private static SearchHit hit(String label) {
        return new SearchHit(UUID.nameUUIDFromBytes(label.getBytes()), LocalDate.of(2026, 10, 1), label, label, 0, 0);
    }
}
