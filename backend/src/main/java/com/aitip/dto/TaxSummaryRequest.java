package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxSummaryRequest(
    TaxPeriod period,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal taxablePercentage
) {}
