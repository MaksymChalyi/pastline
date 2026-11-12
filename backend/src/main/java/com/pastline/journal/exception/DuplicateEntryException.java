package com.pastline.journal.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class DuplicateEntryException extends BaseException {

    public DuplicateEntryException() {
        super(HttpStatus.CONFLICT, "ENTRY_DUPLICATE", "Запис з таким текстом за цю дату вже існує");
    }
}
