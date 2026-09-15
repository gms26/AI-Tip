package com.aitip.dto;

import java.math.BigDecimal;
import java.util.List;

public record TipCurrencyEvolution(
        String currency,
        int totalTipCount,
        int monthsWithActivity,
        BigDecimal currentMedianTipPercentage,
        BigDecimal currentAverageTipPercentage,
        BigDecimal historicalMedianTipPercentage,
        BigDecimal historicalAverageTipPercentage,
        List<TipEvolutionMonth> monthlyTimeline,
        TipEvolutionDirection overallDirection
) {}
