package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TipAnomalyRecord(
        UUID tipId,
        String restaurantName,
        BigDecimal billAmount,
        BigDecimal tipAmount,
        BigDecimal tipPercentage,
        String currency,
        LocalDateTime createdAt,
        TipAnomalyType anomalyType,
        TipAnomalySeverity severity,
        String reason
) {}
