package com.pastline.importing.exception;

import com.pastline.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UnsupportedImportFormatException extends BaseException {

    public UnsupportedImportFormatException() {
        super(HttpStatus.BAD_REQUEST, "IMPORT_UNSUPPORTED_FORMAT", "Підтримуються лише файли .json і .md");
    }
}
