package com.pastline.ask.service;

import com.pastline.ask.dto.AskResponse;
import java.util.List;
import java.util.UUID;

/** Відповідь API плюс дані лише для оцінки: сира відповідь моделі і записи контексту в порядку E1, E2... */
public record AskResult(AskResponse response, String rawAnswer, List<UUID> contextIds) {}
