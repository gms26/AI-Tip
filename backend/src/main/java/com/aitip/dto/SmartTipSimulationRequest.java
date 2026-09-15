package com.aitip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SmartTipSimulationRequest(
        @NotBlank(message = "Currency cannot be blank")
        @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
        String currency,

        @NotNull(message = "Bill amount is required")
        @DecimalMin(value = "0.01", message = "Bill amount must be positive")
        BigDecimal billAmount,

        @NotNull(message = "Tip percentage is required")
        @DecimalMin(value = "0.00", message = "Tip percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Tip percentage cannot exceed 100%")
        BigDecimal tipPercentage,

        String restaurantName,
        ServiceQuality serviceQuality
) {
}
