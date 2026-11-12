package com.pastline.search.service;

import com.pastline.search.dto.SearchHit;
import com.pastline.search.dto.SearchResponse;
import com.pastline.search.exception.InvalidSearchQueryException;
import com.pastline.search.exception.SearchModeNotAvailableException;
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
        List<SearchHit> hits =
                switch (mode) {
                    case LEXICAL -> lexicalRepository.search(userId, q, limit);
                    case SEMANTIC, HYBRID -> throw new SearchModeNotAvailableException(mode.apiName());
                };
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        return new SearchResponse(mode.apiName(), q, tookMs, hits);
    }
}
