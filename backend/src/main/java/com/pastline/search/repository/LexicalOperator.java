package com.pastline.search.repository;

import java.util.Locale;

/**
 * Оператор лексичного запиту. AND: усі слова (синтаксис websearch). OR: будь-яке слово від 3 символів
 * (у конфігурації simple немає стоп-слів).
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
