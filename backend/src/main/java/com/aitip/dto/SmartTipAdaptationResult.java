package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of deterministic behavioral adaptation analysis (Day 32).
 */
public record SmartTipAdaptationResult(
        long feedbackCount,
        long acceptedCount,
        long modifiedCount,
        long customCount,
        long recommendationDecisionCount,
        BigDecimal averageDifferencePercentagePoints,
        SmartTipFeedbackDirection direction,
        boolean adaptationApplied,
        BigDecimal adaptationAdjustment,
        String adaptationMessage
) {
    public SmartTipAdaptationResult {
        if (averageDifferencePercentagePoints != null) {
            averageDifferencePercentagePoints = averageDifferencePercentagePoints.setScale(2, RoundingMode.HALF_UP);
        }
        if (adaptationAdjustment != null) {
            adaptationAdjustment = adaptationAdjustment.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
