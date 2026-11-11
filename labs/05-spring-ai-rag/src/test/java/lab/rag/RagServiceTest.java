package lab.rag;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagServiceTest {

    @Test
    void stripsThinkingBeforeClosingTag() {
        assertEquals("Відповідь [E07].", RagService.stripThinking("Okay, let me think...\n</think>\n\nВідповідь [E07]."));
    }

    @Test
    void keepsContentWithoutThinking() {
        assertEquals("Просто відповідь", RagService.stripThinking("  Просто відповідь "));
    }

    @Test
    void answerWithoutCitationsIsUnverifiedUnlessRefusal() {
        assertEquals("UNVERIFIED", RagService.status("Ти був у Карпатах.", Set.of(), Set.of()));
        assertEquals("VERIFIED", RagService.status("У записах цього немає", Set.of(), Set.of()));
        assertEquals("VERIFIED", RagService.status("Ти був у Карпатах [E20].", Set.of("E20"), Set.of()));
        assertEquals("UNVERIFIED", RagService.status("Ти був на Марсі [E99].", Set.of("E99"), Set.of("E99")));
    }

    @Test
    void extractsCitedIdsWithoutDuplicates() {
        assertEquals(Set.of("E03", "E13"), RagService.citedIds("Зал [E03], біг [E13], знову зал [E03]."));
    }
}
