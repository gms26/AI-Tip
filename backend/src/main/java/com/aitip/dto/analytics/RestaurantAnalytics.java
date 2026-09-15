package com.aitip.dto.analytics;

import com.aitip.dto.ServiceQuality;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Analytics for a specific restaurant.
 *
 * <p>All values are computed from the authenticated user's tips
 * at a single restaurant (case-insensitive match).</p>
 */
public record RestaurantAnalytics(
        String restaurantName,
        int visitCount,
        BigDecimal averageTipPercentage,
        BigDecimal medianTipPercentage,
        BigDecimal averageTipAmount,
        LocalDateTime lastVisit,
        BigDecimal lastTipPercentage,
        ServiceQuality mostCommonServiceQuality
) {}
