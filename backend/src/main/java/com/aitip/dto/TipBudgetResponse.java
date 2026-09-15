package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a tip budget configuration.
 */
public record TipBudgetResponse(
        UUID id,
        String currency,
        BigDecimal monthlyLimit,
        BigDecimal warningThreshold,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
