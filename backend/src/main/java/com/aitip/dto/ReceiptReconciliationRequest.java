package com.aitip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptReconciliationRequest(
        @Positive(message = "Bill amount must be positive")
        BigDecimal billAmount,

        @NotBlank(message = "Restaurant name cannot be blank")
        String restaurantName,

        @NotBlank(message = "Currency cannot be blank")
        String currency,

        LocalDate receiptDate,

        @PositiveOrZero(message = "Tip amount must be zero or positive")
        BigDecimal tipAmount
) {}
