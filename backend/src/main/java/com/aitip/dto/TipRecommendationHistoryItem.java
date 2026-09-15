package com.aitip.dto;

import java.time.LocalDateTime;

public record TipRecommendationHistoryItem(
        TipRecommendationType recommendationType,
        String currency,
        TipRecommendationAction action,
        LocalDateTime createdAt,
        LocalDateTime snoozedUntil
) {
}
