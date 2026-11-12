package com.pastline.shared.exception;

import com.pastline.shared.web.RequestIdFilter;
import java.util.List;

/** Єдиний формат помилок API. */
public record ErrorResponse(String code, String message, String requestId, List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, RequestIdFilter.current(), List.of());
    }
}
