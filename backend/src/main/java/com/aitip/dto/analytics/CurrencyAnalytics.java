package com.aitip.dto.analytics;

import java.math.BigDecimal;

/**
 * Per-currency monetary breakdown for analytics.
 *
 * <p><b>Currency safety rule:</b> All monetary fields in this record
 * apply to a single currency only. Cross-currency monetary aggregation
 * is never performed.</p>
 */
public record CurrencyAnalytics(
        String currency,
        int tipCount,
        BigDecimal totalTipAmount,
        BigDecimal averageTipAmount,
        BigDecimal medianTipAmount
) {}
