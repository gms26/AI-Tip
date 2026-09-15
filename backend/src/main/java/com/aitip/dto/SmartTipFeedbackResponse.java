package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Response payload for POST /api/smart-tip/feedback (Day 32).
 *
 * <p>Returns the deterministic classification and difference in percentage points.</p>
 */
public record SmartTipFeedbackResponse(
        SmartTipFeedbackType feedbackType,
        BigDecimal suggestedTipPercentage,
        BigDecimal chosenTipPercentage,
        BigDecimal differencePercentagePoints
) {
    public SmartTipFeedbackResponse {
        if (suggestedTipPercentage != null) {
            suggestedTipPercentage = suggestedTipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (chosenTipPercentage != null) {
            chosenTipPercentage = chosenTipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (differencePercentagePoints != null) {
            differencePercentagePoints = differencePercentagePoints.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
