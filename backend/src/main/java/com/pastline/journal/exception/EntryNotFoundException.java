package com.pastline.journal.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/** І для неіснуючого, і для чужого запису: не розкриваємо, що запис з таким id існує. */
public class EntryNotFoundException extends BaseException {

    public EntryNotFoundException() {
        super(HttpStatus.NOT_FOUND, "ENTRY_NOT_FOUND", "Запис не знайдено");
    }
}
