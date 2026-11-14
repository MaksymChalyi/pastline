package com.pastline.search.dto;

import java.util.List;

/** pendingCount — записи без актуального вектора (лише semantic і hybrid, інакше null). */
public record SearchResponse(String mode, String query, long tookMs, Integer pendingCount, List<SearchHit> hits) {}
