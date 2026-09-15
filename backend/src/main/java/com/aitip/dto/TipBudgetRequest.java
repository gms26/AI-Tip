package com.aitip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a tip budget.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code currency} — required, validated as ISO 4217 in the service layer</li>
 *   <li>{@code monthlyLimit} — required, must be > 0</li>
 *   <li>{@code warningThreshold} — required, must be > 0 and ≤ 100</li>
 * </ul></p>
 */
public record TipBudgetRequest(
        @NotBlank(message = "Currency is required")
        String currency,

        @NotNull(message = "Monthly limit is required")
        @DecimalMin(value = "0.01", message = "Monthly limit must be greater than 0")
        BigDecimal monthlyLimit,

        @NotNull(message = "Warning threshold is required")
        @DecimalMin(value = "0.01", message = "Warning threshold must be greater than 0")
        @DecimalMax(value = "100", message = "Warning threshold must be at most 100")
        BigDecimal warningThreshold
) {}
