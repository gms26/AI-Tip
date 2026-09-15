package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Summary response for GET /api/smart-tip/feedback (Day 32).
 *
 * <p>Exposes aggregate recommendation interaction facts and behavioral direction
 * strictly scoped to the authenticated user and specified currency.</p>
 */
public record SmartTipFeedbackSummaryResponse(
        String currency,
        long feedbackCount,
        long acceptedCount,
        long modifiedCount,
        long customCount,
        BigDecimal averageDifferencePercentagePoints,
        SmartTipFeedbackDirection direction,
        boolean adaptationApplied,
        BigDecimal adaptationAdjustment
) {
    public SmartTipFeedbackSummaryResponse {
        if (averageDifferencePercentagePoints != null) {
            averageDifferencePercentagePoints = averageDifferencePercentagePoints.setScale(2, RoundingMode.HALF_UP);
        }
        if (adaptationAdjustment != null) {
            adaptationAdjustment = adaptationAdjustment.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
