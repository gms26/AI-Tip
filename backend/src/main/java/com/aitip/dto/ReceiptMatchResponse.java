package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReceiptMatchResponse(
        UUID tipId,
        String restaurantName,
        BigDecimal billAmount,
        BigDecimal tipAmount,
        BigDecimal tipPercentage,
        String currency,
        LocalDateTime createdAt,
        int matchScore,
        List<String> matchReasons
) {}
