package com.pastline.benchmark;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Метрики ранжування для одного запиту. Чисті функції: перевіряються юніт-тестами на ручних прикладах.
 *
 * <ul>
 *   <li>Hit@k: чи є хоча б один релевантний запис серед перших k;
 *   <li>RR@10: 1 / позиція першого релевантного (0, якщо його немає в top-10); середнє по запитах = MRR@10;
 *   <li>Recall@10: знайдені релевантні в top-10, поділені на min(10, кількість релевантних). Без нормування запит із
 *       30 релевантними не міг би отримати більше 10/30, і метрика залежала б від розміру еталону, а не від пошуку.
 * </ul>
 */
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
