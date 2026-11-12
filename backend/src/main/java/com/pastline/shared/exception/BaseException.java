package com.pastline.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Базова очікувана помилка: HTTP-статус і код для клієнта. Конкретні помилки наслідують її. */
@Getter
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected BaseException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
