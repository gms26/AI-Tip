package com.aitip.dto;

import java.math.BigDecimal;

public record CurrencyTipSummary(
    String currency,
    Integer tipCount,
    BigDecimal totalTips,
    BigDecimal averageTip,
    BigDecimal medianTip,
    BigDecimal estimatedTaxableTips
) {}
