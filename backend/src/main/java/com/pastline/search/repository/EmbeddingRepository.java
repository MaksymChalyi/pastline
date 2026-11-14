package com.pastline.search.repository;

import com.pastline.search.dto.SearchHit;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Вектори записів через JDBC (літерал '[...]'::vector). Вектор актуальний, лише коли збігаються content_hash і модель. */
@Repository
@RequiredArgsConstructor
public class EmbeddingRepository {

    /** Запис, якому потрібен (новий) вектор. */
    public record StaleEntry(UUID id, String title, String content, String contentHash) {}

    private static final String STALE_CONDITION = """
            (x.entry_id IS NULL OR x.content_hash <> e.content_hash OR x.model <> :model)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public List<StaleEntry> findStale(String model, int limit) {
        String sql = """
                SELECT e.id, e.title, e.content, e.content_hash
                FROM journal_entries e
                LEFT JOIN entry_embeddings x ON x.entry_id = e.id
                WHERE %s
                ORDER BY e.created_at, e.id
                LIMIT :limit
                """.formatted(STALE_CONDITION);
        return jdbc.query(
                sql,
                new MapSqlParameterSource("model", model).addValue("limit", limit),
                (rs, i) -> new StaleEntry(
                        rs.getObject("id", UUID.class),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("content_hash")));
    }

    public int countStale(UUID userId, String model) {
        String sql = """
                SELECT count(*)
                FROM journal_entries e
                LEFT JOIN entry_embeddings x ON x.entry_id = e.id
                WHERE e.user_id = :userId AND %s
                """.formatted(STALE_CONDITION);
        Integer count = jdbc.queryForObject(
                sql, new MapSqlParameterSource("model", model).addValue("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    /** Хеш — того тексту, з якого пораховано вектор: якщо запис змінили під час обчислення, вектор одразу застарілий. */
    public void saveReady(UUID entryId, float[] vector, String model, String contentHash, Instant now) {
        jdbc.update("""
                INSERT INTO entry_embeddings (entry_id, embedding, model, content_hash, status, error, updated_at)
                VALUES (:entryId, CAST(:embedding AS vector), :model, :hash, 'READY', NULL, :now)
                ON CONFLICT (entry_id) DO UPDATE
                SET embedding = EXCLUDED.embedding, model = EXCLUDED.model, content_hash = EXCLUDED.content_hash,
                    status = 'READY', error = NULL, updated_at = EXCLUDED.updated_at
                """, params(entryId, model, contentHash, now).addValue("embedding", toLiteral(vector)));
    }

    /** FAILED з тим самим хешем і моделлю не вважається застарілим: зіпсований запис не крутиться в циклі. */
    public void saveFailed(UUID entryId, String model, String contentHash, String error, Instant now) {
        jdbc.update("""
                INSERT INTO entry_embeddings (entry_id, embedding, model, content_hash, status, error, updated_at)
                VALUES (:entryId, NULL, :model, :hash, 'FAILED', :error, :now)
                ON CONFLICT (entry_id) DO UPDATE
                SET embedding = NULL, model = EXCLUDED.model, content_hash = EXCLUDED.content_hash,
                    status = 'FAILED', error = EXCLUDED.error, updated_at = EXCLUDED.updated_at
                """, params(entryId, model, contentHash, now).addValue("error", error));
    }

    public int deleteForUser(UUID userId) {
        return jdbc.update("""
                DELETE FROM entry_embeddings x
                USING journal_entries e
                WHERE e.id = x.entry_id AND e.user_id = :userId
                """, new MapSqlParameterSource("userId", userId));
    }

    /** Точний пошук (без ANN-індексу) серед актуальних векторів одного користувача. */
    public List<SearchHit> search(UUID userId, float[] query, String model, int limit) {
        String sql = """
                SELECT e.id, e.entry_date, e.title, left(e.content, :snippetLength + 1) AS snippet,
                       1 - (x.embedding <=> CAST(:query AS vector)) AS score
                FROM entry_embeddings x
                JOIN journal_entries e ON e.id = x.entry_id
                WHERE e.user_id = :userId
                  AND x.status = 'READY'
                  AND x.model = :model
                  AND x.content_hash = e.content_hash
                ORDER BY x.embedding <=> CAST(:query AS vector), e.id
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("query", toLiteral(query))
                .addValue("model", model)
                .addValue("limit", limit)
                .addValue("snippetLength", Snippets.LENGTH);
        int[] rank = {0};
        return jdbc.query(
                sql,
                params,
                (rs, i) -> new SearchHit(
                        rs.getObject("id", UUID.class),
                        rs.getObject("entry_date", LocalDate.class),
                        rs.getString("title"),
                        Snippets.of(rs.getString("snippet")),
                        rs.getDouble("score"),
                        ++rank[0]));
    }

    private static MapSqlParameterSource params(UUID entryId, String model, String hash, Instant now) {
        return new MapSqlParameterSource()
                .addValue("entryId", entryId)
                .addValue("model", model)
                .addValue("hash", hash)
                .addValue("now", java.sql.Timestamp.from(now));
    }

    static String toLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 10).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
