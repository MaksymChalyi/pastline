package com.pastline.search.repository;

/** Сніпет однаковий для всіх режимів: SQL бере на символ більше, щоб знати, чи текст обрізано. */
final class Snippets {

    static final int LENGTH = 200;

    private Snippets() {}

    static String of(String text) {
        return text.length() > LENGTH ? text.substring(0, LENGTH) + "…" : text;
    }
}
