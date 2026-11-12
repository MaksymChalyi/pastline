package com.pastline.search.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/** Режим уже є в API, але ще не реалізований (semantic у 6.6, hybrid у 6.7). */
public class SearchModeNotAvailableException extends BaseException {

    public SearchModeNotAvailableException(String mode) {
        super(HttpStatus.BAD_REQUEST, "MODE_NOT_AVAILABLE", "Режим пошуку " + mode + " ще недоступний");
    }
}
