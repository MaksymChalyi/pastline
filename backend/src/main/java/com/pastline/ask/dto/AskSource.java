package com.pastline.ask.dto;

import java.time.LocalDate;
import java.util.UUID;

/** ref: позначка у тексті відповіді ("E2"), id: справжній запис, на який вона вказує. */
public record AskSource(String ref, UUID id, LocalDate entryDate, String title, String snippet) {}
