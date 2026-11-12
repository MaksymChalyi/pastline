package com.pastline.auth.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class EmailTakenException extends BaseException {

    public EmailTakenException() {
        super(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Користувач з таким email уже існує");
    }
}
