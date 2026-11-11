package lab.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CosineTest {

    private static final double EPS = 1e-9;

    @Test
    void identicalVectorsGiveOne() {
        float[] v = {0.3f, -1.2f, 2.5f};
        assertEquals(1.0, Cosine.similarity(v, v), EPS);
    }

    @Test
    void orthogonalVectorsGiveZero() {
        assertEquals(0.0, Cosine.similarity(new float[]{1, 0}, new float[]{0, 1}), EPS);
    }

    @Test
    void oppositeVectorsGiveMinusOne() {
        assertEquals(-1.0, Cosine.similarity(new float[]{1, 2, 3}, new float[]{-1, -2, -3}), EPS);
    }

    @Test
    void lengthOfVectorDoesNotMatterOnlyDirection() {
        // Той самий напрям, але вектор у 10 разів довший: cosine однаковий.
        assertEquals(1.0, Cosine.similarity(new float[]{1, 2}, new float[]{10, 20}), EPS);
    }

    @Test
    void differentLengthThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Cosine.similarity(new float[]{1, 2}, new float[]{1, 2, 3}));
    }

    @Test
    void zeroVectorThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Cosine.similarity(new float[]{0, 0}, new float[]{1, 2}));
    }
}
