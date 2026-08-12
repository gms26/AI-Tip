package com.aitip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for AI tip recommendations.
 *
 * <p>Validates all required context before reaching the AI.
 * The AI is not the source of truth for bill amount validation.</p>
 */
public record AiSuggestionRequest(
        
        @NotNull(message = "Bill amount is required")
        @DecimalMin(value = "0.01", message = "Bill amount must be greater than 0")
        BigDecimal billAmount,
        
        @NotBlank(message = "Restaurant/Service type is required")
        String restaurantType,
        
        @NotNull(message = "Service quality is required")
        ServiceQuality serviceQuality,
        
        @NotBlank(message = "Country is required")
        String country,
        
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,
        
        String occasion,
        
        /** Optional restaurant name for personalization lookups. Added in Day 4. */
        String restaurantName
) {
}
