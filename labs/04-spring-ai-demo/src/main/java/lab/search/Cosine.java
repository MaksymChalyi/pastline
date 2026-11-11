package lab.search;

/** cos(a, b) = Σ(aᵢ·bᵢ) / (‖a‖·‖b‖), результат у [-1, 1]. */
public final class Cosine {

    private Cosine() {
    }

    public static double similarity(float[] a, float[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Вектори не можуть бути null");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException("Різна довжина векторів: " + a.length + " і " + b.length);
        }
        if (a.length == 0) {
            throw new IllegalArgumentException("Порожні вектори");
        }

        double normA = norm(a);
        double normB = norm(b);
        if (normA == 0 || normB == 0) {
            throw new IllegalArgumentException("Нульовий вектор: cosine не визначений");
        }
        return dot(a, b) / (normA * normB);
    }

    /** Суми в double: у float похибка округлення накопичується. */
    private static double dot(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (double) a[i] * b[i];
        }
        return sum;
    }

    public static double norm(float[] v) {
        return Math.sqrt(dot(v, v));
    }
}
