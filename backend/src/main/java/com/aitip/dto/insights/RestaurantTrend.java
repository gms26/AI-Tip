package com.aitip.dto.insights;

import java.math.BigDecimal;

public record RestaurantTrend(
        String restaurantName,
        Integer tipCount,
        BigDecimal averageTipPercentage,
        BigDecimal medianTipPercentage,
        BigDecimal totalTipAmount,
        String currency,
        String trendDirection
) {}
