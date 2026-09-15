package com.aitip.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TipHistorySearchRequest(
        String restaurantName,
        String currency,
        ServiceQuality serviceQuality,
        LocalDate startDate,
        LocalDate endDate,
        
        @Min(0) @Max(100)
        Integer minTipPercentage,
        
        @Min(0) @Max(100)
        Integer maxTipPercentage,
        
        @PositiveOrZero
        BigDecimal minBillAmount,
        
        @PositiveOrZero
        BigDecimal maxBillAmount,
        
        String sortBy,
        String sortDirection,
        
        @Min(0)
        Integer page,
        
        @Min(1) @Max(100)
        Integer size
) {
    public TipHistorySearchRequest {
        if (minTipPercentage != null && maxTipPercentage != null && minTipPercentage > maxTipPercentage) {
            throw new IllegalArgumentException("minTipPercentage cannot be greater than maxTipPercentage");
        }
        if (minBillAmount != null && maxBillAmount != null && minBillAmount.compareTo(maxBillAmount) > 0) {
            throw new IllegalArgumentException("minBillAmount cannot be greater than maxBillAmount");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }
    }
}
