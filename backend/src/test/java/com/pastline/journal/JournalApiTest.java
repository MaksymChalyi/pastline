package com.pastline.journal;

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
import java.time.LocalDate;
import java.util.UUID;
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
class JournalApiTest {

    private static final String ENTRIES = "/api/v1/entries";

    @Autowired
    MockMvc mvc;

    /** Кожен тест працює з новим користувачем, тому кількості записів передбачувані. */
    MockHttpSession alice;

    @BeforeEach
    void setUp() throws Exception {
        alice = newUserSession();
    }

    @Test
    void createReturnsEntryWithoutInternalFields() throws Exception {
        create(alice, "Перший день", "Сьогодні почав щоденник.", "2026-11-01")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Перший день"))
                .andExpect(jsonPath("$.content").value("Сьогодні почав щоденник."))
                .andExpect(jsonPath("$.entryDate").value("2026-11-01"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.contentHash").doesNotExist());
    }

    @Test
    void getUpdateAndDeleteOwnEntry() throws Exception {
        String id = createId(alice, null, "Чернетка", "2026-11-02");

        mvc.perform(get(ENTRIES + "/" + id).session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").doesNotExist());

        mvc.perform(put(ENTRIES + "/" + id)
                        .with(csrf())
                        .session(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Оновлено", "Новий текст", "2026-11-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Оновлено"))
                .andExpect(jsonPath("$.content").value("Новий текст"))
                .andExpect(jsonPath("$.entryDate").value("2026-11-03"));

        mvc.perform(delete(ENTRIES + "/" + id).with(csrf()).session(alice)).andExpect(status().isNoContent());
        mvc.perform(get(ENTRIES + "/" + id).session(alice))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND"));
    }

    @Test
    void listIsNewestFirstAndPaginated() throws Exception {
        createId(alice, null, "Запис 1", "2026-10-01");
        createId(alice, null, "Запис 3", "2026-10-03");
        createId(alice, null, "Запис 2", "2026-10-02");

        mvc.perform(get(ENTRIES).param("size", "2").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].entryDate").value("2026-10-03"))
                .andExpect(jsonPath("$.items[1].entryDate").value("2026-10-02"))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get(ENTRIES).param("size", "2").param("page", "1").session(alice))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].entryDate").value("2026-10-01"));
    }

    @Test
    void listFiltersByDateRangeInclusive() throws Exception {
        createId(alice, null, "Вересень", "2026-09-30");
        createId(alice, null, "Початок жовтня", "2026-10-01");
        createId(alice, null, "Кінець жовтня", "2026-10-31");
        createId(alice, null, "Листопад", "2026-11-01");

        mvc.perform(get(ENTRIES)
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].content").value("Кінець жовтня"))
                .andExpect(jsonPath("$.items[1].content").value("Початок жовтня"));
    }

    @Test
    void invalidEntryReturnsFieldErrors() throws Exception {
        create(alice, "x".repeat(201), "   ", "2026-11-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void futureDateIsRejected() throws Exception {
        create(alice, null, "Із майбутнього", LocalDate.now().plusDays(3).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void sameTextForSameDateIsDuplicateEvenWithDifferentLineEndings() throws Exception {
        create(alice, null, "Рядок 1\nРядок 2", "2026-11-05").andExpect(status().isCreated());
        create(alice, "Інший заголовок", "  Рядок 1\r\nРядок 2  ", "2026-11-05")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENTRY_DUPLICATE"));
        // той самий текст за іншу дату дозволений
        create(alice, null, "Рядок 1\nРядок 2", "2026-11-06").andExpect(status().isCreated());
    }

    @Test
    void invalidListParametersAreRejected() throws Exception {
        mvc.perform(get(ENTRIES).param("size", "101").session(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get(ENTRIES)
                        .param("from", "2026-11-02")
                        .param("to", "2026-11-01")
                        .session(alice))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ENTRIES).param("from", "01.11.2026").session(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("from"));
    }

    @Test
    void malformedIdIsBadRequest() throws Exception {
        mvc.perform(get(ENTRIES + "/not-a-uuid").session(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void entriesRequireSession() throws Exception {
        mvc.perform(get(ENTRIES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(post(ENTRIES)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "Текст", "2026-11-01")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherUserCannotSeeChangeOrDeleteEntry() throws Exception {
        String id = createId(alice, null, "Особисте Аліси", "2026-11-01");
        MockHttpSession bob = newUserSession();

        mvc.perform(get(ENTRIES + "/" + id).session(bob))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND"));
        mvc.perform(put(ENTRIES + "/" + id)
                        .with(csrf())
                        .session(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "Зламано", "2026-11-01")))
                .andExpect(status().isNotFound());
        mvc.perform(delete(ENTRIES + "/" + id).with(csrf()).session(bob)).andExpect(status().isNotFound());
        mvc.perform(get(ENTRIES).session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));

        // запис Аліси не змінився
        mvc.perform(get(ENTRIES + "/" + id).session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Особисте Аліси"));
    }

    private ResultActions create(MockHttpSession session, String title, String content, String date) throws Exception {
        return mvc.perform(post(ENTRIES)
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(title, content, date)));
    }

    private String createId(MockHttpSession session, String title, String content, String date) throws Exception {
        String body = create(session, title, content, date)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private MockHttpSession newUserSession() throws Exception {
        String email = "journal-" + UUID.randomUUID() + "@example.com";
        String credentials = "{\"email\":\"%s\",\"password\":\"correct-horse-1\"}".formatted(email);
        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    /** Рядки в JSON екрануються вручну лише для \n, \r і лапок: тестові тексти прості. */
    private static String json(String title, String content, String date) {
        StringBuilder sb = new StringBuilder("{");
        if (title != null) {
            sb.append("\"title\":\"").append(escape(title)).append("\",");
        }
        return sb.append("\"content\":\"")
                .append(escape(content))
                .append("\",\"entryDate\":\"")
                .append(date)
                .append("\"}")
                .toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
