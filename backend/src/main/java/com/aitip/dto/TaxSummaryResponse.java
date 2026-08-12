package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TaxSummaryResponse(
    TaxPeriod period,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal totalTips,
    Integer tipCount,
    BigDecimal averageTip,
    BigDecimal medianTip,
    BigDecimal estimatedTaxableTips,
    BigDecimal taxablePercentage,
    List<CurrencyTipSummary> currencyBreakdown,
    String disclaimer
) {}
