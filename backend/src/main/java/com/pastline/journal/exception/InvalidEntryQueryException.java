package com.pastline.journal.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidEntryQueryException extends BaseException {

    public InvalidEntryQueryException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
