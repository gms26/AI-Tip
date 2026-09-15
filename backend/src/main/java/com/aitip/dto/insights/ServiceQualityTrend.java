package com.aitip.dto.insights;

import java.math.BigDecimal;

public record ServiceQualityTrend(
        Integer tipCount,
        BigDecimal averageTipPercentage,
        BigDecimal medianTipPercentage
) {}
