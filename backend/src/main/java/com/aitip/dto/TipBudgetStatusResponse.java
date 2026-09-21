package com.aitip.dto;

import java.math.BigDecimal;

/**
 * Response DTO for the deterministic budget status calculation.
 *
 * <p>All financial fields are backend-calculated using BigDecimal.
 * Groq must never override these values.</p>
 */
public record TipBudgetStatusResponse(
        String currency,
        BigDecimal monthlyLimit,
        BigDecimal currentMonthTips,
        BigDecimal remainingBudget,
        BigDecimal percentageUsed,
        BigDecimal warningThreshold,
        TipBudgetStatus status,
        int tipCount,
        BudgetConfidence confidence,
        String message
) {}
