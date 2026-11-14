package com.pastline.ask;

import static org.assertj.core.api.Assertions.assertThat;

import com.pastline.ask.service.CitationValidator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CitationValidatorTest {

    private static final Set<String> ALLOWED = Set.of("E1", "E2", "E3");

    @Test
    void sentencesWithKnownCitationsAreKept() {
        CitationValidator.Result r =
                CitationValidator.validate("Ти бігав у парку [E1]. Потім купив ОВДП [E2, E3].", ALLOWED);
        assertThat(r.keptSentences()).hasSize(2);
        assertThat(r.citedRefs()).containsExactlyInAnyOrder("E1", "E2", "E3");
        assertThat(r.removedSentences()).isZero();
        assertThat(r.answer()).isEqualTo("Ти бігав у парку [E1]. Потім купив ОВДП [E2, E3].");
    }

    @Test
    void fabricatedCitationRemovesTheSentence() {
        CitationValidator.Result r = CitationValidator.validate("Ти бігав у парку [E1]. Ти купив яхту [E99].", ALLOWED);
        assertThat(r.answer()).isEqualTo("Ти бігав у парку [E1].");
        assertThat(r.citedRefs()).containsExactly("E1");
        assertThat(r.removedSentences()).isEqualTo(1);
    }

    @Test
    void oneFabricatedRefSpoilsTheWholeSentence() {
        CitationValidator.Result r = CitationValidator.validate("Ти бігав і плавав [E1, E42].", ALLOWED);
        assertThat(r.keptSentences()).isEmpty();
        assertThat(r.removedSentences()).isEqualTo(1);
    }

    @Test
    void sentenceWithoutCitationIsRemoved() {
        CitationValidator.Result r = CitationValidator.validate("Ось що я знайшов.\nТи бігав у парку [E1]!", ALLOWED);
        assertThat(r.answer()).isEqualTo("Ти бігав у парку [E1]!");
        assertThat(r.removedSentences()).isEqualTo(1);
    }

    @Test
    void citationAfterTheFullStopBelongsToThePreviousSentence() {
        CitationValidator.Result r =
                CitationValidator.validate("Ти бігав у парку. [E1] Потім пішов додому. [E2]", ALLOWED);
        assertThat(r.keptSentences()).containsExactly("Ти бігав у парку. [E1]", "Потім пішов додому. [E2]");
        assertThat(r.removedSentences()).isZero();
    }

    @Test
    void otherBracketFormatsAreNotCitations() {
        CitationValidator.Result r =
                CitationValidator.validate("Ти бігав [1]. Ти плавав [2026-03-14]. Ти спав (E1).", ALLOWED);
        assertThat(r.keptSentences()).isEmpty();
        assertThat(r.removedSentences()).isEqualTo(3);
    }

    @Test
    void emptyTextGivesNothing() {
        CitationValidator.Result r = CitationValidator.validate("  ", ALLOWED);
        assertThat(r.keptSentences()).isEmpty();
        assertThat(r.removedSentences()).isZero();
    }
}
