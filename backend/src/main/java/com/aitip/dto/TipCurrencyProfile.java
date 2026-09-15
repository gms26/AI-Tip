package com.aitip.dto;

import java.math.BigDecimal;

/**
 * Per-currency tipping profile statistics.
 *
 * <p><b>Currency isolation:</b> Each currency's statistics are calculated
 * independently. Monetary amounts are never mixed across currencies.</p>
 */
public record TipCurrencyProfile(
        String currency,
        int tipCount,
        BigDecimal medianTipPercentage,
        BigDecimal averageTipPercentage,
        BigDecimal averageTipAmount,
        BigDecimal medianTipAmount,
        BigDecimal minTipPercentage,
        BigDecimal maxTipPercentage,
        TipBehaviorType behaviorType,
        TipStyle tipStyle
) {}
