package com.pastline.search.repository;

import com.pastline.search.dto.SearchHit;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Повнотекстовий пошук PostgreSQL. Native SQL через JDBC, а не JPA: результат не сутність, а рядок із рангом, і
 * колонка search_vector у сутність не потрапляє.
 */
@Repository
@RequiredArgsConstructor
public class LexicalSearchRepository {

    // websearch_to_tsquery не падає на довільному вводі користувача: лапки = фраза, "or", "-слово"
    private static final String SQL = """
            SELECT e.id, e.entry_date, e.title, left(e.content, :snippetLength + 1) AS snippet,
                   ts_rank(e.search_vector, q) AS score
            FROM journal_entries e, websearch_to_tsquery('simple', :query) q
            WHERE e.user_id = :userId
              AND e.search_vector @@ q
            ORDER BY score DESC, e.entry_date DESC, e.id
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public List<SearchHit> search(UUID userId, String query, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("query", query)
                .addValue("limit", limit)
                .addValue("snippetLength", Snippets.LENGTH);
        int[] rank = {0};
        return jdbc.query(
                SQL,
                params,
                (rs, i) -> new SearchHit(
                        rs.getObject("id", UUID.class),
                        rs.getObject("entry_date", LocalDate.class),
                        rs.getString("title"),
                        Snippets.of(rs.getString("snippet")),
                        rs.getDouble("score"),
                        ++rank[0]));
    }
}
