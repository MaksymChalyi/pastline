package com.pastline.importing.service;

import com.pastline.importing.dto.ImportError;
import com.pastline.importing.dto.ImportReport;
import com.pastline.importing.exception.InvalidImportFileException;
import com.pastline.importing.parser.ImportFormat;
import com.pastline.importing.parser.JsonEntryParser;
import com.pastline.importing.parser.MarkdownEntryParser;
import com.pastline.importing.parser.ParsedItem;
import com.pastline.journal.dto.EntryRequest;
import com.pastline.journal.exception.DuplicateEntryException;
import com.pastline.journal.service.JournalService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Імпорт у три кроки: перевірка в пам'яті, звірка з базою одним запитом, вставка пачками.
 * Повторний імпорт безпечний завдяки ключу дедуплікації (user_id, entry_date, хеш тексту).
 */
@Service
@RequiredArgsConstructor
public class ImportService {

    public static final int MAX_ENTRIES = 5_000;
    public static final int MAX_ERRORS = 100;
    public static final int BATCH_SIZE = 100;

    private final JsonEntryParser jsonParser;
    private final MarkdownEntryParser markdownParser;
    private final JournalService journalService;
    private final Validator validator;

    public ImportReport importFile(UUID userId, String filename, byte[] bytes, boolean dryRun) {
        ImportFormat format = ImportFormat.fromFilename(filename);
        if (bytes.length == 0) {
            throw new InvalidImportFileException("Файл порожній");
        }
        List<ParsedItem> items =
                switch (format) {
                    case JSON -> jsonParser.parse(bytes);
                    case MARKDOWN -> markdownParser.parse(decode(bytes));
                };
        if (items.size() > MAX_ENTRIES) {
            throw new InvalidImportFileException("Забагато записів: максимум " + MAX_ENTRIES + " за один імпорт");
        }

        int duplicates = 0;
        int failed = 0;
        LocalDate firstDate = null;
        LocalDate lastDate = null;
        List<ImportError> errors = new ArrayList<>();
        Map<String, EntryRequest> candidates = new LinkedHashMap<>();

        // 1. Перевірка кожного запису в пам'яті: валідація і дублікати всередині файлу
        for (int i = 0; i < items.size(); i++) {
            ParsedItem item = items.get(i);
            String error = item.error() != null ? item.error() : validate(item.request());
            if (error != null) {
                failed++;
                if (errors.size() < MAX_ERRORS) {
                    errors.add(new ImportError(i, error));
                }
                continue;
            }
            EntryRequest request = item.request();
            LocalDate date = request.entryDate();
            firstDate = firstDate == null || date.isBefore(firstDate) ? date : firstDate;
            lastDate = lastDate == null || date.isAfter(lastDate) ? date : lastDate;

            String key = JournalService.key(date, JournalService.contentHash(request.content()));
            if (candidates.putIfAbsent(key, request) != null) {
                duplicates++;
            }
        }

        // 2. Дублікати з базою: один запит на весь файл
        if (!candidates.isEmpty()) {
            Set<String> existing = journalService.existingKeys(userId, firstDate, lastDate);
            int before = candidates.size();
            candidates.keySet().removeAll(existing);
            duplicates += before - candidates.size();
        }

        // 3. Запис пачками
        List<EntryRequest> toCreate = new ArrayList<>(candidates.values());
        int created = 0;
        if (dryRun) {
            created = toCreate.size();
        } else {
            for (int from = 0; from < toCreate.size(); from += BATCH_SIZE) {
                List<EntryRequest> batch = toCreate.subList(from, Math.min(from + BATCH_SIZE, toCreate.size()));
                try {
                    created += journalService.createAll(userId, batch);
                } catch (DataIntegrityViolationException e) {
                    // Паралельний імпорт встиг вставити частину пачки: ця пачка по одному запису
                    for (EntryRequest request : batch) {
                        try {
                            journalService.create(userId, request);
                            created++;
                        } catch (DuplicateEntryException | DataIntegrityViolationException duplicate) {
                            duplicates++;
                        }
                    }
                }
            }
        }
        return new ImportReport(dryRun, items.size(), created, duplicates, failed, firstDate, lastDate, errors);
    }

    /** Ті самі правила, що й для POST /entries: анотації EntryRequest плюс заборона дати в майбутньому. */
    private String validate(EntryRequest request) {
        Set<ConstraintViolation<EntryRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            return violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
        }
        if (journalService.isInFuture(request.entryDate())) {
            return "entryDate: дата не може бути в майбутньому";
        }
        return null;
    }

    /** UTF-8 і без BOM, який додають деякі редактори Windows. */
    private static String decode(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        return text.startsWith("﻿") ? text.substring(1) : text;
    }
}
