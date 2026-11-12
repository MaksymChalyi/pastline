package com.pastline.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pastline.TestcontainersConfiguration;
import com.pastline.support.TestSessions;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SearchApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    MockHttpSession alice;

    @BeforeEach
    void setUp() throws Exception {
        alice = TestSessions.newUser(mvc);
    }

    @Test
    void findsEntryByWordInContentOrTitle() throws Exception {
        createEntry(alice, null, "Ранкова пробіжка в парку.", "2026-10-01");
        createEntry(alice, "Відпустка", "Купили квитки до моря.", "2026-10-02");
        createEntry(alice, null, "Купив ОВДП на місяць.", "2026-10-03");

        search(alice, "парку")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("lexical"))
                .andExpect(jsonPath("$.query").value("парку"))
                .andExpect(jsonPath("$.tookMs").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.hits.length()").value(1))
                .andExpect(jsonPath("$.hits[0].snippet").value("Ранкова пробіжка в парку."))
                .andExpect(jsonPath("$.hits[0].rank").value(1));
        search(alice, "ВІДПУСТКА")
                .andExpect(jsonPath("$.hits.length()").value(1))
                .andExpect(jsonPath("$.hits[0].title").value("Відпустка"));
    }

    @Test
    void returnsEmptyListWhenNothingMatches() throws Exception {
        createEntry(alice, null, "Ранкова пробіжка в парку.", "2026-10-01");
        search(alice, "басейн")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits.length()").value(0));
    }

    /** Обмеження конфігурації 'simple': інша форма слова лексично не збігається. Це міряє експеримент. */
    @Test
    void otherWordFormsAreNotMatched() throws Exception {
        createEntry(alice, null, "Сьогодні була пробіжка.", "2026-10-01");
        search(alice, "пробіжка").andExpect(jsonPath("$.hits.length()").value(1));
        search(alice, "пробіжку").andExpect(jsonPath("$.hits.length()").value(0));
    }

    @Test
    void moreRelevantEntryRanksHigher() throws Exception {
        createEntry(alice, null, "Довгий день: робота, магазин, дзвінок мамі і трохи футбол увечері.", "2026-10-02");
        createEntry(alice, "Футбол", "Футбол з друзями, потім ще футбол у дворі.", "2026-10-01");

        search(alice, "футбол")
                .andExpect(jsonPath("$.hits.length()").value(2))
                .andExpect(jsonPath("$.hits[0].title").value("Футбол"))
                .andExpect(jsonPath("$.hits[0].rank").value(1))
                .andExpect(jsonPath("$.hits[1].rank").value(2));
    }

    @Test
    void supportsWebSearchSyntax() throws Exception {
        createEntry(alice, null, "Ранкова пробіжка в парку.", "2026-10-01");
        createEntry(alice, null, "Купив ОВДП на місяць.", "2026-10-02");
        createEntry(alice, null, "Прогулянка в парку з собакою.", "2026-10-03");

        search(alice, "пробіжка or овдп").andExpect(jsonPath("$.hits.length()").value(2));
        search(alice, "парку -собакою").andExpect(jsonPath("$.hits.length()").value(1));
        search(alice, "\"в парку\"").andExpect(jsonPath("$.hits.length()").value(2));
    }

    @Test
    void longContentIsShortenedInSnippet() throws Exception {
        createEntry(alice, null, "слово " + "текст ".repeat(100), "2026-10-01");
        String body = search(alice, "слово").andReturn().getResponse().getContentAsString();
        String snippet = JsonPath.read(body, "$.hits[0].snippet");
        assertThat(snippet).hasSize(201).endsWith("…");
    }

    @Test
    void editedEntryIsFoundByNewText() throws Exception {
        String id = createEntry(alice, null, "Старий текст про велосипед.", "2026-10-01");
        mvc.perform(put("/api/v1/entries/" + id)
                        .with(csrf())
                        .session(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Новий текст про басейн.\",\"entryDate\":\"2026-10-01\"}"))
                .andExpect(status().isOk());

        search(alice, "велосипед").andExpect(jsonPath("$.hits.length()").value(0));
        search(alice, "басейн").andExpect(jsonPath("$.hits.length()").value(1));
    }

    @Test
    void otherUsersEntriesAreNeverFound() throws Exception {
        MockHttpSession bob = TestSessions.newUser(mvc);
        createEntry(bob, null, "Секретний запис Боба про парк.", "2026-10-01");

        search(alice, "парк").andExpect(jsonPath("$.hits.length()").value(0));
        search(bob, "парк").andExpect(jsonPath("$.hits.length()").value(1));
    }

    @Test
    void invalidParametersAreRejected() throws Exception {
        search(alice, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/search").session(alice)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search")
                        .param("q", "парк")
                        .param("limit", "51")
                        .session(alice))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search")
                        .param("q", "парк")
                        .param("mode", "magic")
                        .session(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void searchRequiresSession() throws Exception {
        mvc.perform(get("/api/v1/search").param("q", "парк"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    /**
     * На кількох рядках PostgreSQL слушно обирає seq scan, тому його вимкнено в межах транзакції: тест доводить, що
     * GIN-індекс придатний для запиту. Реальний план на корпусі знімається EXPLAIN ANALYZE окремо.
     */
    @Test
    void ginIndexIsUsableForFullTextQuery() {
        String plan = new TransactionTemplate(transactionManager).execute(tx -> {
            jdbc.execute("SET LOCAL enable_seqscan = off");
            List<String> lines = jdbc.queryForList(
                    "EXPLAIN SELECT id FROM journal_entries "
                            + "WHERE search_vector @@ websearch_to_tsquery('simple', 'парк')",
                    String.class);
            return String.join("\n", lines);
        });
        assertThat(plan).contains("journal_entries_search_idx");
    }

    private ResultActions search(MockHttpSession session, String q) throws Exception {
        return mvc.perform(get("/api/v1/search").param("q", q).session(session));
    }

    private String createEntry(MockHttpSession session, String title, String content, String date) throws Exception {
        String titleJson = title == null ? "" : "\"title\":\"" + title + "\",";
        String body = mvc.perform(post("/api/v1/entries")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + titleJson + "\"content\":\"" + content + "\",\"entryDate\":\"" + date + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
