package com.aitip.dto;

import java.util.List;

public record TipHistorySearchResponse(
        List<TipHistoryRecord> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
