package lab.search;


import lab.data.Entry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Вектори й токени записів рахуються один раз, далі кожен запит шукається обома методами. */
public class SearchIndex {

    public static final int TOP_K = 5;

    private final List<Entry> entries;
    private final List<float[]> vectors;
    private final List<Set<String>> tokens;

    public SearchIndex(List<Entry> entries, List<float[]> vectors) {
        if (entries.size() != vectors.size()) {
            throw new IllegalArgumentException("Записів " + entries.size() + ", векторів " + vectors.size());
        }
        this.entries = entries;
        this.vectors = vectors;
        this.tokens = entries.stream().map(e -> Lexical.tokens(e.text())).toList();
    }

    public List<Hit> semantic(float[] queryVector) {
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            hits.add(new Hit(entries.get(i).id(), Cosine.similarity(queryVector, vectors.get(i))));
        }
        return top(hits);
    }

    /** Записи без жодного спільного слова не вважаються знайденими. */
    public List<Hit> lexical(String queryText) {
        Set<String> queryTokens = Lexical.tokens(queryText);
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            int shared = Lexical.sharedWords(queryTokens, tokens.get(i));
            if (shared > 0) {
                hits.add(new Hit(entries.get(i).id(), shared));
            }
        }
        return top(hits);
    }

    /** При рівній оцінці — за id, щоб порядок був стабільним. */
    private static List<Hit> top(List<Hit> hits) {
        return hits.stream()
                .sorted(Comparator.comparingDouble(Hit::score).reversed().thenComparing(Hit::id))
                .limit(TOP_K)
                .toList();
    }
}
