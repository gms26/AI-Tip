package com.aitip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for tip optimization analysis.
 *
 * <p><b>Fields:</b></p>
 * <ul>
 *   <li>{@code currency} â€” required, validated as ISO 4217 in service layer</li>
 *   <li>{@code currentTipPercentage} â€” required, 0â€“100</li>
 *   <li>{@code monthlyBudget} â€” optional user-supplied assumption for monthly estimation.
 *       This is NOT the user's actual Day 17 budget; it is a hypothetical input.</li>
 *   <li>{@code billAmount} â€” optional, for bill-level tip comparison</li>
 * </ul>
 */
public record TipOptimizationRequest(
        @NotBlank(message = "Currency is required")
        String currency,

        @NotNull(message = "Current tip percentage is required")
        @DecimalMin(value = "0", message = "Tip percentage must be >= 0")
        @DecimalMax(value = "100", message = "Tip percentage must be <= 100")
        BigDecimal currentTipPercentage,

        @DecimalMin(value = "0.01", message = "Monthly budget must be greater than 0")
        BigDecimal monthlyBudget,

        @DecimalMin(value = "0.01", message = "Bill amount must be greater than 0")
        BigDecimal billAmount
) {}
