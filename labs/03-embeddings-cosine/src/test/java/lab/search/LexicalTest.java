package lab.search;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LexicalTest {

    @Test
    void lowercasesAndDropsStopWordsAndPunctuation() {
        assertEquals(Set.of("робив", "після", "свят"), Lexical.tokens("Що я робив після свят?"));
    }

    @Test
    void keepsApostropheInsideWord() {
        assertEquals(Set.of("п'ятірку"), Lexical.tokens("п'ятірку!"));
    }

    @Test
    void countsDistinctSharedWords() {
        Set<String> query = Lexical.tokens("Коли я записався в зал?");
        Set<String> entry = Lexical.tokens("Записався в зал біля дому. Зал хороший.");
        assertEquals(2, Lexical.sharedWords(query, entry));
    }
}
