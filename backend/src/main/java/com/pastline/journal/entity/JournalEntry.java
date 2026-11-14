package com.pastline.journal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * Запис щоденника. Власник — userId, а не зв'язок з User: journal не залежить від сутностей auth.
 * Persistable: id призначається в Java, і без isNew() Spring Data робив би зайвий SELECT перед кожним INSERT.
 */
@Entity
@Table(name = "journal_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // для JPA
public class JournalEntry implements Persistable<UUID> {

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

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean persisted;

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

    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        persisted = true;
    }
}
