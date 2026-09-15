package com.aitip.dto;

import java.util.List;

public record TipRecommendationHistoryResponse(
        List<TipRecommendationHistoryItem> actions,
        int total
) {
}
