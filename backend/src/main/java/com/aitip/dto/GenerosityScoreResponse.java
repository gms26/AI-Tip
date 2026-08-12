package com.aitip.dto;

import java.math.BigDecimal;

/**
 * DTO representing a user's deterministic Generosity Score based on historical tipping.
 *
 * @param score               The calculated generosity score [0-100]. Null if no history.
 * @param category            The categorical description of the score (e.g. CONSERVATIVE, MODERATE, GENEROUS, VERY_GENEROUS). Null if no history.
 * @param medianTipPercentage The median of all historical tip percentages. Null if no history.
 * @param meanTipPercentage   The mean of all historical tip percentages. Null if no history.
 * @param totalTips           Total number of tips logged by the user.
 * @param ratedTips           The number of tips that were included in this calculation (for future proofing, currently equals totalTips for score logic).
 * @param confidence          Confidence in the score based on sample size (LOW, MEDIUM, HIGH).
 * @param message             A neutral natural-language interpretation of the score.
 */
public record GenerosityScoreResponse(
        Integer score,
        String category,
        BigDecimal medianTipPercentage,
        BigDecimal meanTipPercentage,
        Integer totalTips,
        Integer ratedTips,
        String confidence,
        String message
) {
}
