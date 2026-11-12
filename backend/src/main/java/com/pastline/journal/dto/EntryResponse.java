package com.pastline.journal.dto;

import com.pastline.journal.entity.JournalEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** userId і contentHash назовні не віддаються. */
public record EntryResponse(
        UUID id, String title, String content, LocalDate entryDate, Instant createdAt, Instant updatedAt) {

    public static EntryResponse from(JournalEntry e) {
        return new EntryResponse(
                e.getId(), e.getTitle(), e.getContent(), e.getEntryDate(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
