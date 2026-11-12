package com.pastline.search.service;

import com.pastline.ai.port.EmbeddingModelPort;
import com.pastline.search.dto.ReindexResponse;
import com.pastline.search.dto.SearchHit;
import com.pastline.search.dto.SearchResponse;
import com.pastline.search.exception.InvalidSearchQueryException;
import com.pastline.search.repository.EmbeddingRepository;
import com.pastline.search.repository.LexicalSearchRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Текст запиту не логується: він може містити особисту інформацію. */
@Service
public class SearchService {

    public static final int MAX_LIMIT = 50;
    public static final int MAX_QUERY_LENGTH = 500;
    /** Скільки кандидатів кожен режим дає гібридному злиттю. */
    public static final int HYBRID_CANDIDATES = 50;

    private final LexicalSearchRepository lexicalRepository;
    private final EmbeddingRepository embeddingRepository;
    private final EmbeddingModelPort embeddingModel;
    private final int rrfK;

    public SearchService(
            LexicalSearchRepository lexicalRepository,
            EmbeddingRepository embeddingRepository,
            EmbeddingModelPort embeddingModel,
            @Value("${pastline.search.rrf-k:60}") int rrfK) {
        if (rrfK < 1) {
            throw new IllegalArgumentException("pastline.search.rrf-k має бути >= 1");
        }
        this.lexicalRepository = lexicalRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingModel = embeddingModel;
        this.rrfK = rrfK;
    }

    public SearchResponse search(UUID userId, String query, SearchMode mode, int limit, boolean debug) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) {
            throw new InvalidSearchQueryException("q не може бути порожнім");
        }
        if (q.length() > MAX_QUERY_LENGTH) {
            throw new InvalidSearchQueryException("q довший за " + MAX_QUERY_LENGTH + " символів");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidSearchQueryException("limit має бути від 1 до " + MAX_LIMIT);
        }

        long start = System.nanoTime();
        List<SearchHit> hits;
        Integer pending = null;
        switch (mode) {
            case LEXICAL -> hits = lexicalRepository.search(userId, q, limit);
            case SEMANTIC -> {
                hits = semantic(userId, q, limit);
                pending = embeddingRepository.countStale(userId, embeddingModel.modelName());
            }
            case HYBRID -> {
                // Спершу semantic: якщо модель недоступна, 503 без зайвого лексичного запиту.
                // Тихого відкату на lexical немає, інакше бенчмарк міряв би не той режим.
                List<SearchHit> semantic = semantic(userId, q, HYBRID_CANDIDATES);
                List<SearchHit> lexical = lexicalRepository.search(userId, q, HYBRID_CANDIDATES);
                hits = RrfFusion.fuse(lexical, semantic, rrfK, limit);
                pending = embeddingRepository.countStale(userId, embeddingModel.modelName());
            }
            default -> throw new IllegalStateException("Невідомий режим " + mode);
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (!debug) {
            hits = hits.stream().map(SearchHit::withoutDebug).toList();
        }
        return new SearchResponse(mode.apiName(), q, tookMs, pending, hits);
    }

    /** Після зміни моделі чи префіксів: старі вектори видаляються, фоновий індексатор рахує нові. */
    public ReindexResponse reindex(UUID userId) {
        return new ReindexResponse(embeddingRepository.deleteForUser(userId));
    }

    private List<SearchHit> semantic(UUID userId, String q, int limit) {
        float[] vector = embeddingModel.embedQuery(q);
        return embeddingRepository.search(userId, vector, embeddingModel.modelName(), limit);
    }
}
