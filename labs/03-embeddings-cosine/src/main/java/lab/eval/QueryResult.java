package lab.eval;


import lab.data.Query;
import lab.search.Hit;

import java.util.List;

public record QueryResult(Query query, List<Hit> semantic, List<Hit> lexical) {

    public int semanticRank() {
        return rank(semantic);
    }

    public int lexicalRank() {
        return rank(lexical);
    }

    public double topCosine() {
        return semantic.getFirst().score();
    }

    public boolean isExpected(String entryId) {
        return query.hasAnswer() && query.expected().contains(entryId);
    }

    private int rank(List<Hit> hits) {
        return Metrics.rank(hits.stream().map(Hit::id).toList(), query.expected());
    }
}
