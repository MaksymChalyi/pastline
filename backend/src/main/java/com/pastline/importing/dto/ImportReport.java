package com.pastline.importing.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Підсумок імпорту. При dryRun=true created означає "було б створено": нічого не збережено.
 *
 * @param errors не більше {@code ImportService.MAX_ERRORS} перших помилок; повна кількість у failed
 */
public record ImportReport(
        boolean dryRun,
        int total,
        int created,
        int duplicates,
        int failed,
        LocalDate firstDate,
        LocalDate lastDate,
        List<ImportError> errors) {}
