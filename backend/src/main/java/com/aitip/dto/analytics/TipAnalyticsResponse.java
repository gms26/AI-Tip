package com.aitip.dto.analytics;

import com.aitip.dto.AnalyticsPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Top-level response DTO for the tip analytics endpoint.
 *
 * <p><b>Currency safety rule:</b> When the user has tips in multiple currencies,
 * the parent-level monetary fields ({@code totalTipAmount}, {@code averageTipAmount})
 * are {@code null} to prevent misleading cross-currency aggregation.
 * The {@code currencyBreakdown} list is always the authoritative source for monetary values.</p>
 *
 * <p><b>Percentage aggregation:</b> Tip percentages are dimensionless and may be
 * safely aggregated across currencies. Fields like {@code averageTipPercentage},
 * {@code medianTipPercentage}, {@code highestTipPercentage}, and {@code lowestTipPercentage}
 * are always populated regardless of the number of currencies.</p>
 */
public record TipAnalyticsResponse(
        AnalyticsPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        int totalTipCount,
        BigDecimal averageTipPercentage,
        BigDecimal medianTipPercentage,
        BigDecimal highestTipPercentage,
        BigDecimal lowestTipPercentage,
        List<RestaurantAnalytics> restaurantInsights,
        List<ServiceQualityAnalytics> serviceQualityInsights,
        List<CurrencyAnalytics> currencyBreakdown,
        List<MonthlyTipTrend> monthlyTrend
) {}
