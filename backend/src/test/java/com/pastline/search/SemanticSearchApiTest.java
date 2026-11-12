package com.pastline.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pastline.TestcontainersConfiguration;
import com.pastline.search.embedding.EmbeddingIndexer;
import com.pastline.support.FakeEmbeddingModel;
import com.pastline.support.TestSessions;
import java.util.UUID;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SemanticSearchApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EmbeddingIndexer indexer;

    @Autowired
    FakeEmbeddingModel fakeModel;

    MockHttpSession alice;

    @BeforeEach
    void setUp() throws Exception {
        alice = TestSessions.newUser(mvc);
    }

    @Test
    void newEntriesArePendingUntilIndexedThenFound() throws Exception {
        createEntry(alice, "Ранкова пробіжка в парку біля річки.");
        createEntry(alice, "Купив облігації на місяць.");

        semantic(alice, "пробіжка парк")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("semantic"))
                .andExpect(jsonPath("$.pendingCount").value(2))
                .andExpect(jsonPath("$.hits.length()").value(0));

        indexer.indexAll();

        semantic(alice, "пробіжка парк")
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.hits.length()").value(2))
                .andExpect(jsonPath("$.hits[0].snippet").value("Ранкова пробіжка в парку біля річки."))
                .andExpect(jsonPath("$.hits[0].rank").value(1));
    }

    @Test
    void scoreIsCosineSimilarity() throws Exception {
        createEntry(alice, "басейн");
        indexer.indexAll();
        String body = semantic(alice, "басейн").andReturn().getResponse().getContentAsString();
        double score = ((Number) JsonPath.read(body, "$.hits[0].score")).doubleValue();
        assertThat(score).isBetween(0.99, 1.0001);
    }

    @Test
    void editedEntryGetsNewVector() throws Exception {
        String id = createEntry(alice, "Катався на велосипеді.");
        indexer.indexAll();

        mvc.perform(put("/api/v1/entries/" + id)
                        .with(csrf())
                        .session(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Плавав у басейні.\",\"entryDate\":\"2026-10-01\"}"))
                .andExpect(status().isOk());

        // старий вектор застарів: запис чекає перерахунку і не знаходиться за старим текстом
        semantic(alice, "велосипеді")
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.hits.length()").value(0));

        indexer.indexAll();
        semantic(alice, "басейні")
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.hits[0].snippet").value("Плавав у басейні."));
    }

    @Test
    void deletedEntryLosesVector() throws Exception {
        String id = createEntry(alice, "Запис для видалення.");
        indexer.indexAll();
        assertThat(vectorRows(id)).isEqualTo(1);

        mvc.perform(delete("/api/v1/entries/" + id).with(csrf()).session(alice)).andExpect(status().isNoContent());
        assertThat(vectorRows(id)).isZero();
    }

    @Test
    void otherUsersVectorsAreNeverFound() throws Exception {
        MockHttpSession bob = TestSessions.newUser(mvc);
        createEntry(bob, "Секретна пробіжка Боба в парку.");
        indexer.indexAll();

        semantic(alice, "пробіжка парку").andExpect(jsonPath("$.hits.length()").value(0));
        semantic(bob, "пробіжка парку").andExpect(jsonPath("$.hits.length()").value(1));
    }

    @Test
    void failedEntryDoesNotBlockOthersAndIsNotRetriedForever() throws Exception {
        String bad = createEntry(alice, "Зламаний запис " + FakeEmbeddingModel.FAIL_MARKER);
        String good = createEntry(alice, "Нормальний запис про парк.");

        indexer.indexAll();

        assertThat(vectorStatus(bad)).isEqualTo("FAILED");
        assertThat(vectorStatus(good)).isEqualTo("READY");
        semantic(alice, "парк").andExpect(jsonPath("$.pendingCount").value(0));
        assertThat(indexer.indexPending()).isZero();
    }

    @Test
    void unavailableModelGivesServiceUnavailableAndKeepsEntriesPending() throws Exception {
        String id = createEntry(alice, "Запис, поки модель лежить.");
        fakeModel.setAvailable(false);
        try {
            semantic(alice, "запис")
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("EMBEDDING_UNAVAILABLE"));
            assertThat(indexer.indexPending()).isZero();
            assertThat(vectorRows(id)).isZero(); // не FAILED: просто чекає, поки модель повернеться
        } finally {
            fakeModel.setAvailable(true);
        }
        indexer.indexAll();
        assertThat(vectorStatus(id)).isEqualTo("READY");
    }

    @Test
    void reindexRemovesOnlyOwnVectors() throws Exception {
        createEntry(alice, "Запис Аліси.");
        MockHttpSession bob = TestSessions.newUser(mvc);
        createEntry(bob, "Запис Боба.");
        indexer.indexAll();

        mvc.perform(post("/api/v1/search/reindex").with(csrf()).session(alice))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.removed").value(1));

        semantic(alice, "запис").andExpect(jsonPath("$.pendingCount").value(1));
        semantic(bob, "запис").andExpect(jsonPath("$.pendingCount").value(0));
    }

    @Test
    void lexicalResponseHasNoPendingCount() throws Exception {
        mvc.perform(get("/api/v1/search").param("q", "парк").session(alice))
                .andExpect(jsonPath("$.pendingCount").doesNotExist());
    }

    private ResultActions semantic(MockHttpSession session, String q) throws Exception {
        return mvc.perform(get("/api/v1/search")
                .param("q", q)
                .param("mode", "semantic")
                .session(session));
    }

    private String createEntry(MockHttpSession session, String content) throws Exception {
        String body = mvc.perform(post("/api/v1/entries")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\",\"entryDate\":\"2026-10-01\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int vectorRows(String entryId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM entry_embeddings WHERE entry_id = ?", Integer.class, UUID.fromString(entryId));
    }

    private String vectorStatus(String entryId) {
        return jdbc.queryForObject(
                "SELECT status FROM entry_embeddings WHERE entry_id = ?", String.class, UUID.fromString(entryId));
    }
}
