package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TipHistoryRecord(
        UUID id,
        String restaurantName,
        BigDecimal billAmount,
        BigDecimal tipAmount,
        BigDecimal tipPercentage,
        String currency,
        ServiceQuality serviceQuality,
        LocalDateTime createdAt
) {
}
