package com.pastline.search.controller;

import com.pastline.auth.security.AuthenticatedUser;
import com.pastline.search.dto.SearchResponse;
import com.pastline.search.service.SearchMode;
import com.pastline.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Один ендпоінт для всіх режимів: бенчмарк і UI порівнюють режими тим самим запитом. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public SearchResponse search(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String q,
            @RequestParam(defaultValue = "lexical") String mode,
            @RequestParam(defaultValue = "10") int limit) {
        return searchService.search(user.id(), q, SearchMode.parse(mode), limit);
    }
}
