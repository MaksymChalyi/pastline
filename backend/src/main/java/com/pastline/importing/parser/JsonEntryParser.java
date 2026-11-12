package com.pastline.importing.parser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pastline.importing.exception.InvalidImportFileException;
import com.pastline.journal.dto.EntryRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Формат: масив об'єктів {@code [{"title": "...", "content": "...", "entryDate": "2026-11-01"}]}. Зайві поля (id,
 * topic у корпусі) ігноруються. Кожен елемент розбирається окремо, тож один зіпсований не зупиняє решту.
 */
@Component
@RequiredArgsConstructor
public class JsonEntryParser {

    private final ObjectMapper objectMapper;

    public List<ParsedItem> parse(byte[] bytes) {
        JsonNode root;
        try {
            root = objectMapper.readTree(bytes);
        } catch (JacksonException e) {
            throw new InvalidImportFileException("Файл не є коректним JSON");
        }
        if (root == null || !root.isArray()) {
            throw new InvalidImportFileException("JSON має бути масивом записів");
        }
        List<ParsedItem> items = new ArrayList<>();
        for (JsonNode node : root) {
            items.add(toItem(node));
        }
        return items;
    }

    private ParsedItem toItem(JsonNode node) {
        if (!node.isObject()) {
            return ParsedItem.failed("Запис має бути JSON-об'єктом");
        }
        JsonItem item;
        try {
            item = objectMapper.treeToValue(node, JsonItem.class);
        } catch (JacksonException e) {
            return ParsedItem.failed("Некоректні типи полів запису");
        }
        LocalDate date = null;
        if (item.entryDate() != null) {
            try {
                date = LocalDate.parse(item.entryDate());
            } catch (DateTimeParseException e) {
                return ParsedItem.failed("entryDate: очікується формат yyyy-MM-dd");
            }
        }
        return ParsedItem.ok(new EntryRequest(item.title(), item.content(), date));
    }

    /** Дата як рядок: помилку формату повертаємо для конкретного запису, а не для всього файлу. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonItem(String title, String content, String entryDate) {}
}
