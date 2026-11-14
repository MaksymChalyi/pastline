package com.pastline.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BenchmarkMetricsTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();
    private static final UUID D = UUID.randomUUID();

    @Test
    void firstRelevantRankIsOneBased() {
        assertThat(BenchmarkMetrics.firstRelevantRank(List.of(A, B, C), Set.of(B)))
                .isEqualTo(2);
        assertThat(BenchmarkMetrics.firstRelevantRank(List.of(A, B, C), Set.of(D)))
                .isZero();
        assertThat(BenchmarkMetrics.firstRelevantRank(List.of(), Set.of(A))).isZero();
    }

    @Test
    void relevantBeyondDepthDoesNotCount() {
        List<UUID> ranked = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ranked.add(UUID.randomUUID());
        }
        ranked.add(A); // 11-те місце
        assertThat(BenchmarkMetrics.firstRelevantRank(ranked, Set.of(A))).isZero();
        assertThat(BenchmarkMetrics.recall(ranked, Set.of(A))).isZero();
    }

    @Test
    void hitAtKAndReciprocalRank() {
        assertThat(BenchmarkMetrics.hitAt(1, 1)).isTrue();
        assertThat(BenchmarkMetrics.hitAt(1, 2)).isFalse();
        assertThat(BenchmarkMetrics.hitAt(5, 5)).isTrue();
        assertThat(BenchmarkMetrics.hitAt(10, 0)).isFalse();
        assertThat(BenchmarkMetrics.reciprocalRank(4)).isEqualTo(0.25);
        assertThat(BenchmarkMetrics.reciprocalRank(0)).isZero();
    }

    @Test
    void recallIsShareOfAllRelevantFound() {
        // 2 з 3 релевантних у видачі
        assertThat(BenchmarkMetrics.recall(List.of(A, D, B), Set.of(A, B, C))).isCloseTo(2.0 / 3, within(1e-12));
    }

    @Test
    void recallIsNormalizedByDepthWhenThereAreMoreRelevantThanDepth() {
        // 12 релевантних, у top-10 лише вони: більше знайти неможливо, отже 1.0, а не 10/12
        List<UUID> relevant = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            relevant.add(UUID.randomUUID());
        }
        assertThat(BenchmarkMetrics.recall(relevant, Set.copyOf(relevant))).isEqualTo(1.0);
        // 5 з 10 можливих
        List<UUID> half = new ArrayList<>(relevant.subList(0, 5));
        for (int i = 0; i < 5; i++) {
            half.add(UUID.randomUUID());
        }
        assertThat(BenchmarkMetrics.recall(half, Set.copyOf(relevant))).isEqualTo(0.5);
    }

    @Test
    void percentileUsesNearestRank() {
        List<Double> values = List.of(5.0, 1.0, 3.0, 2.0, 4.0);
        assertThat(BenchmarkMetrics.percentile(values, 0.5)).isEqualTo(3.0);
        assertThat(BenchmarkMetrics.percentile(values, 0.95)).isEqualTo(5.0);
        assertThat(BenchmarkMetrics.percentile(List.of(), 0.5)).isZero();
    }
}
