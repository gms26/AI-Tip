package com.aitip.dto;

import java.time.LocalDateTime;

public record TipRecommendationActionResponse(
        TipRecommendationType recommendationType,
        String currency,
        TipRecommendationAction action,
        LocalDateTime createdAt,
        LocalDateTime snoozedUntil,
        String message
) {
}
