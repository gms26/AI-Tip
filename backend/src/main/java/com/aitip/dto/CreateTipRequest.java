package com.aitip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO for creating a new tip.
 *
 * <p><b>Purpose:</b> Captures and validates user input for tip calculations.</p>
 *
 * <p><b>Validation Strategy:</b>
 * Fail fast at the controller boundary. A bill cannot be negative or zero.
 * Tip percentage must be between 0 and 100.</p>
 *
 * <p><b>Day 6 â€” serviceQuality:</b>
 * Required for all new tips. Historical tips stored in the DB before Day 6
 * have a nullable column; new tips must always record what service was like.
 * The value must be one of the {@link ServiceQuality} enum values.</p>
 *
 * @param billAmount      The total bill before tip (must be > 0)
 * @param tipPercentage   The tip percentage (0-100)
 * @param restaurantName  Optional restaurant name
 * @param currency        ISO 4217 currency code (defaults to USD if null)
 * @param serviceQuality  Required user rating of service quality
 */
public record CreateTipRequest(

        @NotNull(message = "Bill amount is required")
        @DecimalMin(value = "0.01", message = "Bill amount must be greater than 0")
        BigDecimal billAmount,

        @NotNull(message = "Tip percentage is required")
        @DecimalMin(value = "0.00", message = "Tip percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Tip percentage cannot exceed 100")
        BigDecimal tipPercentage,

        @Size(max = 255, message = "Restaurant name cannot exceed 255 characters")
        String restaurantName,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "Service quality is required")
        ServiceQuality serviceQuality
) {
    /**
     * Compact constructor to set default currency if null.
     */
    public CreateTipRequest {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        } else {
            currency = currency.toUpperCase();
        }
    }
}
