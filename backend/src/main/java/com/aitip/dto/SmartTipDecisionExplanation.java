package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Transparent explanation of the deterministic decision process (Day 33).
 */
public record SmartTipDecisionExplanation(
        String summary,
        List<SmartTipDecisionFactor> factors,
        BigDecimal baselinePercentage,
        BigDecimal adaptedPercentage,
        boolean adaptationApplied,
        BigDecimal adaptationAdjustment,
        String confidence
) {
    public SmartTipDecisionExplanation {
        if (baselinePercentage != null) {
            baselinePercentage = baselinePercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (adaptedPercentage != null) {
            adaptedPercentage = adaptedPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (adaptationAdjustment != null) {
            adaptationAdjustment = adaptationAdjustment.setScale(2, RoundingMode.HALF_UP);
        }
        if (factors == null) {
            factors = List.of();
        }
    }
}
