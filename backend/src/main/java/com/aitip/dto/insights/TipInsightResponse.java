package com.aitip.dto.insights;

import java.math.BigDecimal;

public record TipInsightResponse(
        String trendDirection,
        BigDecimal recentAveragePercentage,
        BigDecimal historicalAveragePercentage,
        BigDecimal percentageChange,
        Integer sampleSize,
        String confidence,
        String message
) {}
