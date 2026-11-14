package com.pastline.ask.dto;

public enum AskStatus {
    /** Лишилось хоча б одне речення з перевіреним джерелом. */
    ANSWERED,
    /** Записів не знайдено, модель відмовилась або всі речення відкинула перевірка джерел. */
    NO_EVIDENCE
}
