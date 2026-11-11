package lab.eval;

import lab.search.SearchIndex;

import java.util.List;

/** Rank рахується з 1; 0 — правильного запису немає в top-5. */
public final class Metrics {

    private Metrics() {
    }

    public static int rank(List<String> topIds, List<String> expected) {
        for (int i = 0; i < Math.min(SearchIndex.TOP_K, topIds.size()); i++) {
            if (expected.contains(topIds.get(i))) {
                return i + 1;
            }
        }
        return 0;
    }

    public static double hitAt(List<Integer> ranks, int k) {
        if (ranks.isEmpty()) {
            return 0;
        }
        return ranks.stream().filter(r -> r >= 1 && r <= k).count() / (double) ranks.size();
    }

    public static double mrr(List<Integer> ranks) {
        if (ranks.isEmpty()) {
            return 0;
        }
        return ranks.stream().mapToDouble(r -> r == 0 ? 0 : 1.0 / r).average().orElse(0);
    }
}
