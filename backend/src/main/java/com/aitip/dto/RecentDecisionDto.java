package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Compact DTO representing a single recent tip decision for the learning insights view (Day 35).
 *
 * <p>Contains only the fields necessary for the frontend to render a recent-decisions list.
 * No sensitive or unnecessary information is exposed.</p>
 */
public record RecentDecisionDto(
        LocalDateTime createdAt,
        String restaurantName,
        ServiceQuality serviceQuality,
        BigDecimal suggestedTipPercentage,
        BigDecimal chosenTipPercentage,
        BigDecimal differencePercentagePoints,
        SmartTipFeedbackType feedbackType,
        String recommendationType
) {}
