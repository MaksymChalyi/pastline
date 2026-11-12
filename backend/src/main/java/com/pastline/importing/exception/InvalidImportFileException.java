package com.pastline.importing.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/** Файл не вдається прочитати цілком: тоді імпорт не починається зовсім. */
public class InvalidImportFileException extends BaseException {

    public InvalidImportFileException(String message) {
        super(HttpStatus.BAD_REQUEST, "IMPORT_INVALID_FILE", message);
    }
}
