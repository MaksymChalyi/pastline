package com.pastline.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pastline.TestcontainersConfiguration;
import com.pastline.search.embedding.EmbeddingIndexer;
import com.pastline.support.FakeEmbeddingModel;
import com.pastline.support.TestSessions;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HybridSearchApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    EmbeddingIndexer indexer;

    @Autowired
    FakeEmbeddingModel fakeModel;

    MockHttpSession alice;

    @BeforeEach
    void setUp() throws Exception {
        alice = TestSessions.newUser(mvc);
        createEntry(alice, "пробіжка зранку пробіжка");
        createEntry(alice, "вечірня пробіжка після роботи і довгої наради з командою");
        createEntry(alice, "зранку випив каву");
        indexer.indexAll();
    }

    @Test
    void entryFoundByBothModesIsFirstAndOthersStayInResult() throws Exception {
        String body = hybrid(alice, "пробіжка зранку", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("hybrid"))
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.hits.length()").value(3))
                .andExpect(jsonPath("$.hits[0].snippet").value("пробіжка зранку пробіжка"))
                .andExpect(jsonPath("$.hits[0].debug.lexicalRank").value(1))
                .andExpect(jsonPath("$.hits[0].debug.semanticRank").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // "зранку випив каву" лексично не знаходиться (слова запиту через AND), але семантичний режим його дає
        List<Object> lexicalRanks =
                JsonPath.read(body, "$.hits[?(@.snippet == 'зранку випив каву')].debug.lexicalRank");
        List<Object> semanticRanks =
                JsonPath.read(body, "$.hits[?(@.snippet == 'зранку випив каву')].debug.semanticRank");
        assertThat(lexicalRanks).containsExactly((Object) null);
        assertThat(semanticRanks).doesNotContainNull().hasSize(1);
    }

    @Test
    void debugFieldsAppearOnlyOnRequest() throws Exception {
        hybrid(alice, "пробіжка", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits[0].debug").doesNotExist());
        hybrid(alice, "пробіжка", true)
                .andExpect(jsonPath("$.hits[0].debug.rrfScore").isNumber());
    }

    @Test
    void limitIsApplied() throws Exception {
        mvc.perform(get("/api/v1/search")
                        .param("q", "пробіжка")
                        .param("mode", "hybrid")
                        .param("limit", "1")
                        .session(alice))
                .andExpect(jsonPath("$.hits.length()").value(1))
                .andExpect(jsonPath("$.hits[0].rank").value(1));
    }

    @Test
    void otherUsersEntriesAreNeverFused() throws Exception {
        MockHttpSession bob = TestSessions.newUser(mvc);
        createEntry(bob, "секретна пробіжка Боба");
        indexer.indexAll();

        hybrid(bob, "пробіжка", false).andExpect(jsonPath("$.hits.length()").value(1));
        hybrid(alice, "секретна Боба", false)
                .andExpect(jsonPath("$.hits[?(@.snippet == 'секретна пробіжка Боба')]")
                        .doesNotExist());
    }

    @Test
    void unavailableModelGivesServiceUnavailable() throws Exception {
        fakeModel.setAvailable(false);
        try {
            hybrid(alice, "пробіжка", false)
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("EMBEDDING_UNAVAILABLE"));
        } finally {
            fakeModel.setAvailable(true);
        }
    }

    private ResultActions hybrid(MockHttpSession session, String q, boolean debug) throws Exception {
        return mvc.perform(get("/api/v1/search")
                .param("q", q)
                .param("mode", "hybrid")
                .param("debug", String.valueOf(debug))
                .session(session));
    }

    private void createEntry(MockHttpSession session, String content) throws Exception {
        mvc.perform(post("/api/v1/entries")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\",\"entryDate\":\"2026-10-01\"}"))
                .andExpect(status().isCreated());
    }
}
