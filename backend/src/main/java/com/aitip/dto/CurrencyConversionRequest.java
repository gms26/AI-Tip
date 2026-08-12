package com.aitip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for currency conversion.
 */
public record CurrencyConversionRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Source currency is required")
        @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
        String sourceCurrency,

        @NotBlank(message = "Target currency is required")
        @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
        String targetCurrency
) {
}
