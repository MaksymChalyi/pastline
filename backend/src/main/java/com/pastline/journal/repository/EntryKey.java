package com.pastline.journal.repository;

import java.time.LocalDate;

/** Ключ дедуплікації без тексту запису: для імпорту достатньо дати й хешу. */
public record EntryKey(LocalDate entryDate, String contentHash) {}
