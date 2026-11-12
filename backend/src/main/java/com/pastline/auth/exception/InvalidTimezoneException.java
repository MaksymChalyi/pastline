package com.pastline.auth.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidTimezoneException extends BaseException {

    public InvalidTimezoneException(String timezone) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Невідомий часовий пояс: " + timezone);
    }
}
