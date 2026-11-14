package com.pastline.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pastline.search.repository.LexicalOperator;
import org.junit.jupiter.api.Test;

class LexicalOperatorTest {

    @Test
    void parsesIgnoringCaseAndSpaces() {
        assertThat(LexicalOperator.parse("and")).isEqualTo(LexicalOperator.AND);
        assertThat(LexicalOperator.parse(" OR ")).isEqualTo(LexicalOperator.OR);
    }

    @Test
    void unknownValueFailsInsteadOfFallingBack() {
        assertThatThrownBy(() -> LexicalOperator.parse("any")).isInstanceOf(IllegalArgumentException.class);
    }
}
