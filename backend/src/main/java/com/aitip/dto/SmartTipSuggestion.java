package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Deterministic suggestion option calculated by the Smart Tip Assistant.
 *
 * <p>All monetary amounts are scaled to 2 decimal places using {@link RoundingMode#HALF_UP}.</p>
 */
public record SmartTipSuggestion(
        SmartTipSuggestionType type,
        BigDecimal tipPercentage,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        String label,
        String reason,
        BigDecimal historicalDifferencePercentagePoints,
        String budgetImpact,
        Boolean isRecommended
) {
    public SmartTipSuggestion {
        if (tipPercentage != null) {
            tipPercentage = tipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (tipAmount != null) {
            tipAmount = tipAmount.setScale(2, RoundingMode.HALF_UP);
        }
        if (totalAmount != null) {
            totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        }
        if (historicalDifferencePercentagePoints != null) {
            historicalDifferencePercentagePoints = historicalDifferencePercentagePoints.setScale(2, RoundingMode.HALF_UP);
        }
        if (isRecommended == null) {
            isRecommended = false;
        }
    }
}
