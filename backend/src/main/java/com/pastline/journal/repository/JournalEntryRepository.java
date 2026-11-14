package com.pastline.journal.repository;

import com.pastline.journal.entity.JournalEntry;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Кожен метод фільтрує за userId у SQL: чужий запис просто не знаходиться. */
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdAndUserId(UUID id, UUID userId);

    List<JournalEntry> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    Page<JournalEntry> findByUserIdAndEntryDateBetween(UUID userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
            SELECT new com.pastline.journal.repository.EntryKey(e.entryDate, e.contentHash)
            FROM JournalEntry e
            WHERE e.userId = :userId AND e.entryDate BETWEEN :from AND :to
            """)
    List<EntryKey> findKeys(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    boolean existsByUserIdAndEntryDateAndContentHash(UUID userId, LocalDate entryDate, String contentHash);

    boolean existsByUserIdAndEntryDateAndContentHashAndIdNot(
            UUID userId, LocalDate entryDate, String contentHash, UUID id);
}
