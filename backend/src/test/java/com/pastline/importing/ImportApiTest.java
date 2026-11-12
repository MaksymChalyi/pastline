package com.pastline.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pastline.TestcontainersConfiguration;
import com.pastline.support.TestSessions;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ImportApiTest {

    private static final Path CORPUS = Path.of("..", "docs", "experiments", "corpus-v1");

    private static final String THREE_ENTRIES = """
            [
              {"title": "Робота", "content": "Закрив задачу з платежами.", "entryDate": "2026-10-01"},
              {"content": "Пробіжка в парку.", "entryDate": "2026-10-02", "topic": "sport"},
              {"content": "Ran 5 km in the morning.", "entryDate": "2026-10-03"}
            ]
            """;

    @Autowired
    MockMvc mvc;

    MockHttpSession alice;

    @BeforeEach
    void setUp() throws Exception {
        alice = TestSessions.newUser(mvc);
    }

    @Test
    void jsonImportCreatesEntries() throws Exception {
        upload(alice, "entries.json", THREE_ENTRIES, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.created").value(3))
                .andExpect(jsonPath("$.duplicates").value(0))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.firstDate").value("2026-10-01"))
                .andExpect(jsonPath("$.lastDate").value("2026-10-03"));

        mvc.perform(get("/api/v1/entries").session(alice))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[2].title").value("Робота"));
    }

    @Test
    void repeatedImportAddsNothing() throws Exception {
        upload(alice, "entries.json", THREE_ENTRIES, false)
                .andExpect(jsonPath("$.created").value(3));
        upload(alice, "entries.json", THREE_ENTRIES, false)
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.duplicates").value(3));
        mvc.perform(get("/api/v1/entries").session(alice))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void dryRunReportsButSavesNothing() throws Exception {
        upload(alice, "entries.json", THREE_ENTRIES, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.created").value(3));
        mvc.perform(get("/api/v1/entries").session(alice))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void dryRunIsTheDefault() throws Exception {
        mvc.perform(multipart("/api/v1/import")
                        .file(file("entries.json", THREE_ENTRIES))
                        .with(csrf())
                        .session(alice))
                .andExpect(jsonPath("$.dryRun").value(true));
        mvc.perform(get("/api/v1/entries").session(alice))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void invalidItemsAreReportedAndValidOnesSaved() throws Exception {
        String json = """
                [
                  {"content": "Нормальний запис.", "entryDate": "2026-10-01"},
                  {"entryDate": "2026-10-02"},
                  {"content": "Погана дата.", "entryDate": "02.10.2026"},
                  {"content": "З майбутнього.", "entryDate": "2999-01-01"},
                  "не об'єкт",
                  {"content": "Ще один нормальний.", "entryDate": "2026-10-05"}
                ]
                """;
        upload(alice, "mixed.json", json, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.failed").value(4))
                .andExpect(jsonPath("$.errors[0].index").value(1))
                .andExpect(jsonPath("$.errors[1].index").value(2))
                .andExpect(jsonPath("$.errors[2].index").value(3))
                .andExpect(jsonPath("$.errors[3].index").value(4));
    }

    @Test
    void duplicatesInsideOneFileAreCountedOnce() throws Exception {
        String json = """
                [
                  {"content": "Рядок 1\\nРядок 2", "entryDate": "2026-10-01"},
                  {"title": "Копія", "content": "  Рядок 1\\r\\nРядок 2 ", "entryDate": "2026-10-01"}
                ]
                """;
        upload(alice, "dup.json", json, false)
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.duplicates").value(1));
    }

    @Test
    void markdownImportSplitsByDateHeadings() throws Exception {
        String md = """
                ## 2026-10-01 Перший день
                Почав вести щоденник.

                ## 2026-10-02
                Другий запис.
                ### Підзаголовок лишається текстом
                Ще рядок.
                """;
        upload(alice, "diary.md", md, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.created").value(2));

        mvc.perform(get("/api/v1/entries").session(alice))
                .andExpect(jsonPath("$.items[0].title").doesNotExist())
                .andExpect(jsonPath("$.items[0].content")
                        .value("Другий запис.\n### Підзаголовок лишається текстом\nЩе рядок."))
                .andExpect(jsonPath("$.items[1].title").value("Перший день"))
                .andExpect(jsonPath("$.items[1].content").value("Почав вести щоденник."));
    }

    @Test
    void markdownProblemsAreReported() throws Exception {
        String md = """
                Текст без заголовка.
                ## вчора
                Запис без дати.
                ## 2026-02-30 Неіснуюча дата
                Текст.
                ## 2026-10-03
                Нормальний запис.
                """;
        upload(alice, "diary.md", md, false)
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.failed").value(3));
    }

    @Test
    void unsupportedOrBrokenFilesAreRejected() throws Exception {
        upload(alice, "notes.txt", "text", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_UNSUPPORTED_FORMAT"));
        upload(alice, "broken.json", "[{\"content\": ", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_INVALID_FILE"));
        upload(alice, "object.json", "{\"content\": \"x\"}", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_INVALID_FILE"));
        upload(alice, "empty.json", "", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_INVALID_FILE"));
    }

    @Test
    void importRequiresSession() throws Exception {
        mvc.perform(multipart("/api/v1/import")
                        .file(file("entries.json", THREE_ENTRIES))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void importedEntriesBelongOnlyToImporter() throws Exception {
        upload(alice, "entries.json", THREE_ENTRIES, false)
                .andExpect(jsonPath("$.created").value(3));
        MockHttpSession bob = TestSessions.newUser(mvc);

        mvc.perform(get("/api/v1/entries").session(bob))
                .andExpect(jsonPath("$.totalItems").value(0));
        // дедуплікація в межах користувача: той самий файл у Боба створює його власні записи
        upload(bob, "entries.json", THREE_ENTRIES, false)
                .andExpect(jsonPath("$.created").value(3));
    }

    @Test
    void corpusIsReproducibleAndImportsFully() throws Exception {
        byte[] corpus = Files.readAllBytes(CORPUS.resolve("corpus.json"));
        String expectedHash = Files.readString(CORPUS.resolve("corpus.sha256")).split("\\s+")[0];
        String actualHash =
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(corpus));
        assertThat(actualHash).as("corpus.json змінено без перегенерації").isEqualTo(expectedHash);

        upload(alice, "corpus.json", corpus, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(520))
                .andExpect(jsonPath("$.created").value(520))
                .andExpect(jsonPath("$.failed").value(0));
        upload(alice, "corpus.json", corpus, false)
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.duplicates").value(520));
    }

    private ResultActions upload(MockHttpSession session, String filename, String content, boolean dryRun)
            throws Exception {
        return upload(session, filename, content.getBytes(StandardCharsets.UTF_8), dryRun);
    }

    private ResultActions upload(MockHttpSession session, String filename, byte[] content, boolean dryRun)
            throws Exception {
        return mvc.perform(multipart("/api/v1/import")
                .file(file(filename, content))
                .param("dryRun", String.valueOf(dryRun))
                .with(csrf())
                .session(session));
    }

    private static MockMultipartFile file(String filename, String content) {
        return file(filename, content.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile file(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content);
    }
}
