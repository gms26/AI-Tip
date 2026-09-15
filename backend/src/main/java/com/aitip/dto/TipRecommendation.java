package com.aitip.dto;

public record TipRecommendation(
        TipRecommendationType type,
        TipRecommendationPriority priority,
        String title,
        String message,
        String currency,
        String supportingValue,
        String supportingValueLabel,
        String action,
        TipRecommendationAction userAction
) {
    public TipRecommendation(TipRecommendationType type, TipRecommendationPriority priority, String title, String message, String currency, String supportingValue, String supportingValueLabel, String action) {
        this(type, priority, title, message, currency, supportingValue, supportingValueLabel, action, null);
    }
}
