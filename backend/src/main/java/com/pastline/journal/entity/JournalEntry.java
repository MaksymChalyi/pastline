package com.pastline.journal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Запис щоденника. Власник зберігається як userId, а не як зв'язок з User: модуль journal не залежить від сутностей
 * auth, а зв'язок гарантує зовнішній ключ у БД.
 */
@Entity
@Table(name = "journal_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // для JPA
public class JournalEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    private String title;

    @Column(nullable = false)
    private String content;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public JournalEntry(
            UUID userId, String title, String content, LocalDate entryDate, String contentHash, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.entryDate = entryDate;
        this.contentHash = contentHash;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String content, LocalDate entryDate, String contentHash, Instant now) {
        this.title = title;
        this.content = content;
        this.entryDate = entryDate;
        this.contentHash = contentHash;
        this.updatedAt = now;
    }
}
