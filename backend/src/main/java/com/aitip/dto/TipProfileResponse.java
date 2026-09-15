package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Deterministic personalized tipping profile.
 *
 * <p><b>Empty/Limited State rules:</b></p>
 * <ul>
 *   <li>If totalTipCount < 5: consistencyScore, behaviorType are null</li>
 *   <li>If totalTipCount < 10: recentTrend is null</li>
 * </ul>
 */
public record TipProfileResponse(
        LocalDateTime generatedAt,
        int totalTipCount,
        List<TipCurrencyProfile> currencyProfiles,
        TipBehaviorType overallBehaviorType,
        TipStyle overallTipStyle,
        BigDecimal historicalMedianTipPercentage,
        BigDecimal historicalAverageTipPercentage,
        BigDecimal tipPercentageMin,
        BigDecimal tipPercentageMax,
        Integer consistencyScore,
        BigDecimal recentMedianTipPercentage,
        String recentTrend,
        List<RestaurantPattern> topRestaurants,
        List<ServiceQualityPattern> topServiceQualities,
        String message,
        String aiExplanation
) {
    public TipProfileResponse {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
