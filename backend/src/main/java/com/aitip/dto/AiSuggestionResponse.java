package com.aitip.dto;

import java.math.BigDecimal;

/**
 * Output DTO for AI tip recommendations.
 *
 * <p><b>Architecture note:</b> The AI only provides the percentages and 
 * the text reason/confidence. The backend calculated the `tipAmount` and 
 * `totalAmount` exactly using BigDecimal to prevent AI arithmetic errors.</p>
 */
public record AiSuggestionResponse(
        BigDecimal recommendedPercentage,
        BigDecimal minimumPercentage,
        BigDecimal maximumPercentage,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        String reason,
        String confidence,
        PersonalizationSource personalizationSource
) {
}
