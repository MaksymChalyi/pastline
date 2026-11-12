package com.pastline.importing.parser;

import com.pastline.importing.exception.UnsupportedImportFormatException;
import java.util.Locale;

public enum ImportFormat {
    JSON,
    MARKDOWN;

    /** Формат за розширенням: Content-Type від браузера для .md ненадійний. */
    public static ImportFormat fromFilename(String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return JSON;
        }
        if (name.endsWith(".md") || name.endsWith(".markdown")) {
            return MARKDOWN;
        }
        throw new UnsupportedImportFormatException();
    }
}
