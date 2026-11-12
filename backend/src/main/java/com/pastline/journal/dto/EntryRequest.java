package com.pastline.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EntryRequest(
        @Size(max = 200) String title,
        @NotBlank @Size(max = 20_000) String content,
        @NotNull LocalDate entryDate) {}
