package com.pastline.benchmark;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Метрики для одного запиту: Hit@k, RR@10 (середнє — MRR@10), Recall@10, нормований на min(10, |релевантних|). */
public final class BenchmarkMetrics {

    public static final int DEPTH = 10;

    private BenchmarkMetrics() {}

    /** Позиція першого релевантного (з 1) у межах DEPTH, або 0, якщо такого немає. */
    public static int firstRelevantRank(List<UUID> ranked, Set<UUID> relevant) {
        for (int i = 0; i < Math.min(DEPTH, ranked.size()); i++) {
            if (relevant.contains(ranked.get(i))) {
                return i + 1;
            }
        }
        return 0;
    }

    public static boolean hitAt(int k, int firstRelevantRank) {
        return firstRelevantRank > 0 && firstRelevantRank <= k;
    }

    public static double reciprocalRank(int firstRelevantRank) {
        return firstRelevantRank == 0 ? 0 : 1.0 / firstRelevantRank;
    }

    public static double recall(List<UUID> ranked, Set<UUID> relevant) {
        if (relevant.isEmpty()) {
            return 0;
        }
        long found = ranked.stream().limit(DEPTH).filter(relevant::contains).count();
        return (double) found / Math.min(DEPTH, relevant.size());
    }

    /** Перцентиль методом найближчого рангу; values не обов'язково відсортовані. */
    public static double percentile(List<Double> values, double p) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
