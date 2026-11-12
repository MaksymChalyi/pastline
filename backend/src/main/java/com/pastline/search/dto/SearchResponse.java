package com.pastline.search.dto;

import java.util.List;

/**
 * @param tookMs час пошуку на сервері; для semantic включає обчислення вектора запиту
 * @param pendingCount записи користувача ще без актуального вектора (лише semantic, інакше null): поки він більший за
 *     0, семантичний пошук бачить не всі записи
 */
public record SearchResponse(String mode, String query, long tookMs, Integer pendingCount, List<SearchHit> hits) {}
