package com.aitip.dto;

import java.math.BigDecimal;

/**
 * A restaurant pattern showing tipping behavior at a specific restaurant.
 *
 * <p>Restaurant names are normalized using {@code trim().toLowerCase()} for
 * grouping, but the original casing of the first occurrence is preserved
 * for display purposes.</p>
 */
public record RestaurantPattern(
        String restaurantName,
        int tipCount,
        BigDecimal averageTipPercentage,
        BigDecimal medianTipPercentage
) {}
