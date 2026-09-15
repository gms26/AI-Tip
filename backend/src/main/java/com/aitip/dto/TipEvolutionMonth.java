package com.aitip.dto;

import java.math.BigDecimal;

public record TipEvolutionMonth(
        String month,
        int tipCount,
        BigDecimal medianTipPercentage,
        BigDecimal averageTipPercentage,
        BigDecimal averageTipAmount,
        BigDecimal medianTipAmount,
        BigDecimal minTipPercentage,
        BigDecimal maxTipPercentage,
        Integer consistencyScore,
        TipStyle tipStyle,
        TipBehaviorType behaviorType,
        int uniqueRestaurantCount
) {}
