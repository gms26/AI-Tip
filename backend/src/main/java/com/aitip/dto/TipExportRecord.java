package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
    "id", "createdAt", "restaurantName", "billAmount", "tipPercentage",
    "tipAmount", "totalAmount", "currency", "serviceQuality"
})
public record TipExportRecord(
        UUID id,
        LocalDateTime createdAt,
        String restaurantName,
        BigDecimal billAmount,
        BigDecimal tipPercentage,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        String currency,
        ServiceQuality serviceQuality
) {}
