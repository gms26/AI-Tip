package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TipEvolutionResponse(
        LocalDateTime generatedAt,
        TipEvolutionPeriod period,
        String currency,
        int totalTipCount,
        int activeMonths,
        TipEvolutionDirection overallDirection,
        BigDecimal currentMedianTipPercentage,
        BigDecimal currentAverageTipPercentage,
        BigDecimal historicalMedianTipPercentage,
        BigDecimal historicalAverageTipPercentage,
        List<TipCurrencyEvolution> currencyTimelines,
        List<TipServiceQualityEvolution> serviceQualityEvolution,
        String message,
        String aiExplanation
) {
    public TipEvolutionResponse {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
