package com.pastline.search.repository;

import java.util.Locale;

/**
 * Як слова запиту поєднуються в лексичному пошуку (параметр pastline.search.lexical-operator).
 *
 * <ul>
 *   <li>AND: запис має містити всі слова; синтаксис websearch (лапки, "or", "-слово");
 *   <li>OR: достатньо одного слова, ранг росте з кількістю збігів, як у класичному BM25-пошуку. Слова коротші за 3
 *       символи відкидаються: у конфігурації 'simple' немає стоп-слів, і "в", "на" збігалися б з кожним записом.
 * </ul>
 */
public enum LexicalOperator {
    AND,
    OR;

    /** Невідоме значення зупиняє запуск, а не перемикає режим мовчки. */
    public static LexicalOperator parse(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "and" -> AND;
            case "or" -> OR;
            default ->
                throw new IllegalArgumentException(
                        "pastline.search.lexical-operator має бути and або or, отримано: " + value);
        };
    }
}
