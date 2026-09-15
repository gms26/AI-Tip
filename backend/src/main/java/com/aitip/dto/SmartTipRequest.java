package com.aitip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for POST /api/smart-tip.
 *
 * <p>Contains the current bill context, currency, optional restaurant name,
 * service quality, and informational current tip percentage.</p>
 */
public record SmartTipRequest(

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "Bill amount is required")
        @DecimalMin(value = "0.01", inclusive = false, message = "Bill amount must be greater than 0.01")
        BigDecimal billAmount,

        @Size(max = 255, message = "Restaurant name cannot exceed 255 characters")
        String restaurantName,

        ServiceQuality serviceQuality,

        @DecimalMin(value = "0.00", message = "Tip percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Tip percentage cannot exceed 100")
        BigDecimal currentTipPercentage
) {
    public SmartTipRequest {
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
        if (restaurantName != null && restaurantName.isBlank()) {
            restaurantName = null;
        }
    }
}
