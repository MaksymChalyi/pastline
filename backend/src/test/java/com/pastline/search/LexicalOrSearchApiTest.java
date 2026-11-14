package com.pastline.search;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pastline.TestcontainersConfiguration;
import com.pastline.support.TestSessions;
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

/** Лексичний пошук з оператором OR (абляція для бенчмарку). Режим AND покриває SearchApiTest. */
@SpringBootTest(properties = "pastline.search.lexical-operator=or")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LexicalOrSearchApiTest {

    @Autowired
    MockMvc mvc;

    MockHttpSession alice;

    @BeforeEach
    void setUp() throws Exception {
        alice = TestSessions.newUser(mvc);
    }

    @Test
    void oneMatchingWordIsEnoughAndMoreMatchesRankHigher() throws Exception {
        String both = createEntry(alice, "Здав аналіз крові, чекаю результати.", "2026-10-01");
        String one = createEntry(alice, "Чекаю на потяг уже годину.", "2026-10-02");
        createEntry(alice, "Ранкова пробіжка в парку.", "2026-10-03");

        // AND не знайшов би нічого: слів "лабораторні" і "відповідь" немає в жодному записі
        search(alice, "здав лабораторні чекаю відповідь")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits.length()").value(2))
                .andExpect(jsonPath("$.hits[0].id").value(both))
                .andExpect(jsonPath("$.hits[1].id").value(one));
    }

    @Test
    void shortWordsAreIgnored() throws Exception {
        createEntry(alice, "Ранкова пробіжка в парку на світанку.", "2026-10-01");

        search(alice, "в на з")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits.length()").value(0));
        search(alice, "в парку").andExpect(jsonPath("$.hits.length()").value(1));
    }

    @Test
    void tsquerySyntaxInInputDoesNotBreakTheQuery() throws Exception {
        createEntry(alice, "Купив ОВДП на місяць.", "2026-10-01");

        search(alice, "овдп & | ! ( ) :* \\ ' \"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits.length()").value(1));
    }

    @Test
    void otherUsersEntriesAreNeverFound() throws Exception {
        MockHttpSession bob = TestSessions.newUser(mvc);
        createEntry(bob, "Секретний запис Боба про парк.", "2026-10-01");

        search(alice, "секретний запис парк")
                .andExpect(jsonPath("$.hits.length()").value(0));
    }

    private ResultActions search(MockHttpSession session, String q) throws Exception {
        return mvc.perform(get("/api/v1/search").param("q", q).session(session));
    }

    private String createEntry(MockHttpSession session, String content, String date) throws Exception {
        String body = mvc.perform(post("/api/v1/entries")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\",\"entryDate\":\"" + date + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
