package com.pastline.ask.service;

import com.pastline.ask.dto.AskResponse;
import java.util.List;
import java.util.UUID;

/**
 * Відповідь API плюс те, що потрібно лише для оцінки H5 і назовні не віддається: сирий текст моделі до перевірки та
 * записи, передані моделі як E1, E2... (у тому ж порядку).
 */
public record AskResult(AskResponse response, String rawAnswer, List<UUID> contextIds) {}
