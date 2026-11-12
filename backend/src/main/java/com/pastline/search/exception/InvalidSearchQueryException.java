package com.pastline.search.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidSearchQueryException extends BaseException {

    public InvalidSearchQueryException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
