package com.pastline.search.service;

import com.pastline.ai.port.EmbeddingModelPort;
import com.pastline.search.dto.ReindexResponse;
import com.pastline.search.dto.SearchHit;
import com.pastline.search.dto.SearchResponse;
import com.pastline.search.exception.InvalidSearchQueryException;
import com.pastline.search.exception.SearchModeNotAvailableException;
import com.pastline.search.repository.EmbeddingRepository;
import com.pastline.search.repository.LexicalSearchRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Текст запиту не логується: він може містити особисту інформацію. */
@Service
@RequiredArgsConstructor
public class SearchService {

    public static final int MAX_LIMIT = 50;
    public static final int MAX_QUERY_LENGTH = 500;

    private final LexicalSearchRepository lexicalRepository;
    private final EmbeddingRepository embeddingRepository;
    private final EmbeddingModelPort embeddingModel;

    public SearchResponse search(UUID userId, String query, SearchMode mode, int limit) {
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
                float[] vector = embeddingModel.embedQuery(q);
                hits = embeddingRepository.search(userId, vector, embeddingModel.modelName(), limit);
                pending = embeddingRepository.countStale(userId, embeddingModel.modelName());
            }
            default -> throw new SearchModeNotAvailableException(mode.apiName());
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        return new SearchResponse(mode.apiName(), q, tookMs, pending, hits);
    }

    /** Після зміни моделі чи префіксів: старі вектори видаляються, фоновий індексатор рахує нові. */
    public ReindexResponse reindex(UUID userId) {
        return new ReindexResponse(embeddingRepository.deleteForUser(userId));
    }
}
