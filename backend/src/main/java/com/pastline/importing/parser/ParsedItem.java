package com.pastline.importing.parser;

import com.pastline.journal.dto.EntryRequest;

/** Результат розбору одного запису файлу: або запит на створення, або причина, чому його не вдалося прочитати. */
public record ParsedItem(EntryRequest request, String error) {

    public static ParsedItem ok(EntryRequest request) {
        return new ParsedItem(request, null);
    }

    public static ParsedItem failed(String error) {
        return new ParsedItem(null, error);
    }
}
