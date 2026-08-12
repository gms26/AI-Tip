package com.aitip.dto;

import java.math.BigDecimal;

/**
 * Overall personalization summary for an authenticated user.
 *
 * <p>All fields are backend-calculated STATISTICS and FACTS.
 * None of these values are AI-generated.</p>
 *
 * <p><b>Design decisions:</b></p>
 * <ul>
 *   <li>{@code medianTipPercentage} is the primary "typical" metric (robust to outliers)</li>
 *   <li>{@code averageTipPercentage} is the arithmetic mean (reported separately)</li>
 *   <li>{@code personalizedPercentage} uses the median, rounded to 1 decimal</li>
 *   <li>{@code message} is a human-readable summary based on tip count thresholds</li>
 * </ul>
 */
public record PersonalizationResponse(
        int tipCount,
        BigDecimal averageTipPercentage,
        BigDecimal medianTipPercentage,
        BigDecimal minimumTipPercentage,
        BigDecimal maximumTipPercentage,
        BigDecimal averageTipAmount,
        BigDecimal personalizedPercentage,
        String message,
        RestaurantInsight restaurantInsight
) {
}
