package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Recent decision memory element (Day 33).
 */
public record SmartTipRecentDecision(
        LocalDateTime date,
        String currency,
        String restaurantName,
        BigDecimal suggestedPercentage,
        BigDecimal chosenPercentage,
        BigDecimal differencePercentagePoints,
        SmartTipFeedbackType feedbackType
) {
    public SmartTipRecentDecision {
        if (suggestedPercentage != null) {
            suggestedPercentage = suggestedPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (chosenPercentage != null) {
            chosenPercentage = chosenPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (differencePercentagePoints != null) {
            differencePercentagePoints = differencePercentagePoints.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
