package com.aitip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for POST /api/smart-tip/feedback (Day 32).
 *
 * <p>Captures the recommendation context and explicit user decision upon saving a tip.</p>
 */
public record SmartTipFeedbackRequest(

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "Bill amount is required")
        @DecimalMin(value = "0.01", inclusive = false, message = "Bill amount must be greater than 0.01")
        BigDecimal billAmount,

        @Size(max = 255, message = "Restaurant name cannot exceed 255 characters")
        String restaurantName,

        ServiceQuality serviceQuality,

        @DecimalMin(value = "0.00", message = "Suggested tip percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Suggested tip percentage cannot exceed 100")
        BigDecimal suggestedTipPercentage,

        @Size(max = 50, message = "Recommendation type cannot exceed 50 characters")
        String suggestedRecommendationType,

        @NotNull(message = "Chosen tip percentage is required")
        @DecimalMin(value = "0.00", message = "Chosen tip percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Chosen tip percentage cannot exceed 100")
        BigDecimal chosenTipPercentage
) {
    public SmartTipFeedbackRequest {
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
        if (restaurantName != null && restaurantName.isBlank()) {
            restaurantName = null;
        } else if (restaurantName != null) {
            restaurantName = restaurantName.trim();
        }
        if (suggestedRecommendationType != null && suggestedRecommendationType.isBlank()) {
            suggestedRecommendationType = null;
        }
    }
}
