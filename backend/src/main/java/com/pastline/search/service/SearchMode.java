package com.pastline.search.service;

import com.pastline.search.exception.InvalidSearchQueryException;
import java.util.Locale;

public enum SearchMode {
    LEXICAL,
    SEMANTIC,
    HYBRID;

    public static SearchMode parse(String value) {
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidSearchQueryException("mode має бути lexical, semantic або hybrid");
        }
    }

    public String apiName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
