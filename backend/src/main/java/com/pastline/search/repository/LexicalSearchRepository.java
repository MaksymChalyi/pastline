package com.pastline.search.repository;

import com.pastline.search.dto.SearchHit;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Повнотекстовий пошук PostgreSQL. Native SQL через JDBC, а не JPA: результат не сутність, а рядок із рангом, і
 * колонка search_vector у сутність не потрапляє.
 */
@Repository
public class LexicalSearchRepository {

    private static final String SELECT = """
            SELECT e.id, e.entry_date, e.title, left(e.content, :snippetLength + 1) AS snippet,
                   ts_rank(e.search_vector, q) AS score
            FROM journal_entries e, %s
            WHERE e.user_id = :userId
              AND e.search_vector @@ q
            ORDER BY score DESC, e.entry_date DESC, e.id
            LIMIT :limit
            """;

    // websearch_to_tsquery не падає на довільному вводі користувача: лапки = фраза, "or", "-слово"
    private static final String AND_QUERY = "websearch_to_tsquery('simple', :query) AS q";

    // Слова розбирає той самий парсер, що будує search_vector; plainto_tsquery сам екранує кожну лексему.
    // Порожній результат дає NULL, і "@@ NULL" не повертає рядків.
    private static final String OR_QUERY = """
            (SELECT string_agg(plainto_tsquery('simple', w)::text, ' | ')::tsquery AS q
                    FROM unnest(tsvector_to_array(to_tsvector('simple', :query))) AS w
                    WHERE char_length(w) >= 3) AS or_query""";

    private final NamedParameterJdbcTemplate jdbc;
    private final String sql;

    public LexicalSearchRepository(
            NamedParameterJdbcTemplate jdbc, @Value("${pastline.search.lexical-operator:and}") String operator) {
        this.jdbc = jdbc;
        this.sql = SELECT.formatted(LexicalOperator.parse(operator) == LexicalOperator.AND ? AND_QUERY : OR_QUERY);
    }

    public List<SearchHit> search(UUID userId, String query, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("query", query)
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
}
