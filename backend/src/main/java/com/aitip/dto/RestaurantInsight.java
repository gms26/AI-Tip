package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Restaurant-specific tipping insight.
 *
 * <p>Contains FACTS and STATISTICS about a user's history at a specific restaurant.
 * These are backend-calculated, not AI-generated.</p>
 */
public record RestaurantInsight(
        String restaurantName,
        int visitCount,
        BigDecimal averageTipPercentage,
        BigDecimal medianTipPercentage,
        BigDecimal lastTipPercentage,
        BigDecimal lastTipAmount,
        LocalDateTime lastVisitDate
) {
}
