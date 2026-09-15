package com.aitip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for PUT /api/smart-tip/personalization (Day 36).
 */
public record PersonalizationUpdateRequest(

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "Enabled flag is required")
        Boolean enabled
) {
    public PersonalizationUpdateRequest {
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
    }
}
