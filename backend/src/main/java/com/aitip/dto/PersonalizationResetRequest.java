package com.aitip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for POST /api/smart-tip/personalization/reset (Day 36).
 */
public record PersonalizationResetRequest(

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency
) {
    public PersonalizationResetRequest {
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
    }
}
