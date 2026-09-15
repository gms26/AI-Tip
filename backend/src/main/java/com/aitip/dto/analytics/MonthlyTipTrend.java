package com.aitip.dto.analytics;

import java.math.BigDecimal;

/**
 * Monthly trend data point for tip analytics.
 *
 * <p>{@code month} is formatted as "YYYY-MM" for easy sorting
 * and frontend chart axis labeling.</p>
 */
public record MonthlyTipTrend(
        String month,
        int tipCount,
        BigDecimal averageTipPercentage
) {}
