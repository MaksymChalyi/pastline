package com.pastline.search.dto;

import java.util.List;

/** tookMs: час виконання пошуку на сервері, потрібен для порівняння затримки режимів. */
public record SearchResponse(String mode, String query, long tookMs, List<SearchHit> hits) {}
