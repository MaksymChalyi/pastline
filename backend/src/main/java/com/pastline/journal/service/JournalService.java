package com.pastline.journal.service;

import com.pastline.journal.dto.EntryRequest;
import com.pastline.journal.entity.JournalEntry;
import com.pastline.journal.exception.DuplicateEntryException;
import com.pastline.journal.exception.EntryNotFoundException;
import com.pastline.journal.exception.InvalidEntryQueryException;
import com.pastline.journal.repository.JournalEntryRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JournalService {

    public static final int MAX_PAGE_SIZE = 100;

    /** Найпізніший часовий пояс на Землі: дата не може бути пізнішою за "сьогодні" ніде у світі. */
    private static final ZoneOffset LATEST_ZONE = ZoneOffset.ofHours(14);

    private static final LocalDate MIN_DATE = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("entryDate"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final JournalEntryRepository repository;
    private final Clock clock;

    @Transactional
    public JournalEntry create(UUID userId, EntryRequest request) {
        String content = normalize(request.content());
        String hash = sha256(content);
        LocalDate date = checkDate(request.entryDate());
        if (repository.existsByUserIdAndEntryDateAndContentHash(userId, date, hash)) {
            throw new DuplicateEntryException();
        }
        return repository.save(new JournalEntry(userId, title(request.title()), content, date, hash, clock.instant()));
    }

    @Transactional(readOnly = true)
    public JournalEntry get(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(EntryNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Page<JournalEntry> list(UUID userId, LocalDate from, LocalDate to, int page, int size) {
        if (page < 0) {
            throw new InvalidEntryQueryException("page має бути >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidEntryQueryException("size має бути від 1 до " + MAX_PAGE_SIZE);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidEntryQueryException("from не може бути пізніше за to");
        }
        // Без "(:from IS NULL OR ...)": PostgreSQL не може визначити тип null-параметра. Межі замість null.
        return repository.findByUserIdAndEntryDateBetween(
                userId,
                from == null ? MIN_DATE : from,
                to == null ? MAX_DATE : to,
                PageRequest.of(page, size, NEWEST_FIRST));
    }

    @Transactional
    public JournalEntry update(UUID userId, UUID id, EntryRequest request) {
        JournalEntry entry = get(userId, id);
        String content = normalize(request.content());
        String hash = sha256(content);
        LocalDate date = checkDate(request.entryDate());
        if (repository.existsByUserIdAndEntryDateAndContentHashAndIdNot(userId, date, hash, id)) {
            throw new DuplicateEntryException();
        }
        entry.update(title(request.title()), content, date, hash, clock.instant());
        return entry;
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(get(userId, id));
    }

    private LocalDate checkDate(LocalDate date) {
        if (date.isAfter(LocalDate.now(clock.withZone(LATEST_ZONE)))) {
            throw new InvalidEntryQueryException("entryDate не може бути в майбутньому");
        }
        return date;
    }

    /** Однакові переноси рядків і без пробілів по краях: інакше той самий текст дав би різні хеші. */
    static String normalize(String content) {
        return content.replace("\r\n", "\n").strip();
    }

    private static String title(String title) {
        return title == null || title.isBlank() ? null : title.strip();
    }

    static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступний", e);
        }
    }
}
