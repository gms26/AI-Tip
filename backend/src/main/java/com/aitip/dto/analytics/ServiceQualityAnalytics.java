package com.aitip.dto.analytics;

import com.aitip.dto.ServiceQuality;

import java.math.BigDecimal;

/**
 * Analytics for a specific service quality rating.
 *
 * <p><b>Note:</b> {@code averageTipAmount} is only meaningful
 * when all tips in this group share a single currency.
 * For multi-currency data, use {@link CurrencyAnalytics} instead.</p>
 */
public record ServiceQualityAnalytics(
        ServiceQuality serviceQuality,
        int tipCount,
        BigDecimal averageTipPercentage,
        BigDecimal medianTipPercentage,
        BigDecimal averageTipAmount
) {}
