package com.aitip.dto.insights;

import java.math.BigDecimal;
import java.util.Map;

public record MonthlyTrend(
        Integer tipCount,
        BigDecimal averageTipPercentage,
        Map<String, BigDecimal> totalTipAmountGroupedByCurrency
) {}
