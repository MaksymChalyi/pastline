package com.pastline.journal.repository;

import com.pastline.journal.entity.JournalEntry;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Кожен метод фільтрує за userId у SQL: чужий запис просто не знаходиться. */
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdAndUserId(UUID id, UUID userId);

    Page<JournalEntry> findByUserIdAndEntryDateBetween(UUID userId, LocalDate from, LocalDate to, Pageable pageable);

    boolean existsByUserIdAndEntryDateAndContentHash(UUID userId, LocalDate entryDate, String contentHash);

    boolean existsByUserIdAndEntryDateAndContentHashAndIdNot(
            UUID userId, LocalDate entryDate, String contentHash, UUID id);
}
