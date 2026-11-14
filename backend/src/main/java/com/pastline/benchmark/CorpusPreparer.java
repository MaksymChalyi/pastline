package com.pastline.benchmark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pastline.ai.port.EmbeddingModelPort;
import com.pastline.auth.service.AuthService;
import com.pastline.importing.dto.ImportReport;
import com.pastline.importing.service.ImportService;
import com.pastline.journal.service.JournalService;
import com.pastline.search.embedding.EmbeddingIndexer;
import com.pastline.search.repository.EmbeddingRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Підготовка корпусу для бенчмарків: користувач, імпорт, очікування всіх векторів, відповідність корпус → БД. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorpusPreparer {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CorpusEntry(String id, String entryDate, String title, String content) {}

    public record Prepared(
            UUID userId,
            List<CorpusEntry> corpus,
            Map<String, UUID> dbIdByCorpusId,
            String corpusSha256,
            int failedEmbeddings) {

        /** Записи корпусу (id у БД), що містять хоча б одне з речень. */
        public Set<UUID> entriesContaining(List<String> sentences) {
            Set<UUID> result = new HashSet<>();
            for (CorpusEntry e : corpus) {
                if (sentences.stream().anyMatch(e.content()::contains)) {
                    result.add(dbIdByCorpusId.get(e.id()));
                }
            }
            return result;
        }
    }

    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final ImportService importService;
    private final EmbeddingIndexer indexer;
    private final EmbeddingRepository embeddingRepository;
    private final EmbeddingModelPort embeddingModel;
    private final JdbcTemplate jdbc;

    public Prepared prepare(Path corpusFile, String userEmail) throws IOException {
        byte[] corpusBytes = Files.readAllBytes(corpusFile);
        List<CorpusEntry> corpus = objectMapper.readValue(corpusBytes, new TypeReference<>() {});

        UUID userId = ensureUser(userEmail);
        ImportReport imported = importService.importFile(userId, "corpus.json", corpusBytes, false);
        log.info(
                "Імпорт: створено {}, вже було {}, помилок {}",
                imported.created(),
                imported.duplicates(),
                imported.failed());
        if (imported.failed() > 0) {
            throw new IllegalStateException("Корпус імпортовано з помилками: " + imported.errors());
        }

        log.info("Індексація векторів...");
        indexer.indexAll();
        int stale = embeddingRepository.countStale(userId, embeddingModel.modelName());
        if (stale > 0) {
            throw new IllegalStateException(
                    stale + " записів без вектора: модель ембедінгів недоступна або індексація неповна");
        }
        return new Prepared(
                userId, corpus, mapCorpusToDb(userId, corpus), sha256(corpusBytes), countFailedEmbeddings(userId));
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private UUID ensureUser(String email) {
        try {
            return authService.loadUserByUsername(email).id();
        } catch (UsernameNotFoundException e) {
            return authService
                    .register(email, UUID.randomUUID().toString(), null)
                    .getId();
        }
    }

    private int countFailedEmbeddings(UUID userId) {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM entry_embeddings x JOIN journal_entries e ON e.id = x.entry_id
                WHERE e.user_id = ? AND x.status = 'FAILED'
                """, Integer.class, userId);
        return n == null ? 0 : n;
    }

    /** Запис корпусу -> запис у БД за ключем дедуплікації (дата + хеш нормалізованого тексту). */
    private Map<String, UUID> mapCorpusToDb(UUID userId, List<CorpusEntry> corpus) {
        Map<String, UUID> dbByKey = new HashMap<>();
        jdbc.query(
                "SELECT id, entry_date, content_hash FROM journal_entries WHERE user_id = ?",
                rs -> {
                    dbByKey.put(
                            JournalService.key(
                                    rs.getObject("entry_date", LocalDate.class), rs.getString("content_hash")),
                            rs.getObject("id", UUID.class));
                },
                userId);
        Map<String, UUID> result = new HashMap<>();
        for (CorpusEntry e : corpus) {
            String key = JournalService.key(LocalDate.parse(e.entryDate()), JournalService.contentHash(e.content()));
            UUID id = dbByKey.get(key);
            if (id == null) {
                throw new IllegalStateException("Запис корпусу " + e.id() + " не знайдено в БД");
            }
            result.put(e.id(), id);
        }
        return result;
    }
}
