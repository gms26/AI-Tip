package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Summary response for GET /api/smart-tip/decision-memory (Day 33).
 */
public record SmartTipDecisionMemoryResponse(
        String currency,
        long totalFeedback,
        long accepted,
        long modified,
        long custom,
        BigDecimal averageDifferencePercentagePoints,
        SmartTipFeedbackDirection direction,
        String confidence,
        CommonChosenRange commonChosenRange,
        List<SmartTipRecentDecision> recentDecisions
) {
    public record CommonChosenRange(
            BigDecimal min,
            BigDecimal max,
            BigDecimal median,
            BigDecimal average
    ) {
        public CommonChosenRange {
            if (min != null) min = min.setScale(2, RoundingMode.HALF_UP);
            if (max != null) max = max.setScale(2, RoundingMode.HALF_UP);
            if (median != null) median = median.setScale(2, RoundingMode.HALF_UP);
            if (average != null) average = average.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public SmartTipDecisionMemoryResponse {
        if (averageDifferencePercentagePoints != null) {
            averageDifferencePercentagePoints = averageDifferencePercentagePoints.setScale(2, RoundingMode.HALF_UP);
        }
        if (recentDecisions == null) {
            recentDecisions = List.of();
        }
    }
}
