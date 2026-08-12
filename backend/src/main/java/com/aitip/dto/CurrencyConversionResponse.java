package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Standardized response for currency conversion operations.
 */
public record CurrencyConversionResponse(
        String sourceCurrency,
        String targetCurrency,
        BigDecimal originalAmount,
        BigDecimal convertedAmount,
        BigDecimal exchangeRate,
        LocalDateTime rateTimestamp,
        String provider
) {
}
