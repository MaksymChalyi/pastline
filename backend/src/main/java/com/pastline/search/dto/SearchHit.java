package com.pastline.search.dto;

import java.time.LocalDate;
import java.util.UUID;

/** rank починається з 1; score залежить від режиму, тож порівнювати його між режимами не можна. */
public record SearchHit(UUID id, LocalDate entryDate, String title, String snippet, double score, int rank) {}
