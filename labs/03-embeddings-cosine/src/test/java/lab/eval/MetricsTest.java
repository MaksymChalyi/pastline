package lab.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsTest {

    @Test
    void rankIsPositionOfFirstExpected() {
        assertEquals(3, Metrics.rank(List.of("E01", "E02", "E12", "E19"), List.of("E12", "E19")));
    }

    @Test
    void rankIsZeroWhenNotInTop5() {
        assertEquals(0, Metrics.rank(List.of("E01", "E02", "E03", "E04", "E05", "E12"), List.of("E12")));
    }

    @Test
    void hitAndMrr() {
        List<Integer> ranks = List.of(1, 2, 0, 5);
        assertEquals(0.25, Metrics.hitAt(ranks, 1), 1e-9);
        assertEquals(0.75, Metrics.hitAt(ranks, 5), 1e-9);
        assertEquals((1 + 0.5 + 0 + 0.2) / 4, Metrics.mrr(ranks), 1e-9);
    }
}
