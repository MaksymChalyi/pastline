package com.pastline.ask.service;

import com.pastline.ask.dto.AskResponse;

/**
 * Відповідь API плюс сирий текст моделі до перевірки. Сирий текст потрібен лише для оцінки H5 і назовні не
 * віддається.
 */
public record AskResult(AskResponse response, String rawAnswer, int contextEntries) {}
