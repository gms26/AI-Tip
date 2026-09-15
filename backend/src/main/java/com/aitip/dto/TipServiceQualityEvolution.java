package com.aitip.dto;

import java.math.BigDecimal;
import java.util.Map;

public record TipServiceQualityEvolution(
        ServiceQuality serviceQuality,
        Map<String, BigDecimal> monthlyValues,
        BigDecimal overallAverageTipPercentage
) {}
