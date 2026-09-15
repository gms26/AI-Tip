package com.aitip.dto;

import com.aitip.entity.TipGoalPeriod;
import com.aitip.entity.TipGoalType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTipGoalRequest(
        @NotNull TipGoalType goalType,
        @NotNull @DecimalMin(value = "0.01", message = "Target value must be strictly positive") BigDecimal targetValue,
        String currency,
        @NotNull TipGoalPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        String restaurantName,
        ServiceQuality serviceQuality
) {
    public CreateTipGoalRequest {
        if (period == TipGoalPeriod.CUSTOM) {
            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException("CUSTOM period requires startDate and endDate");
            }
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("startDate cannot be after endDate");
            }
        }
        
        if (goalType == TipGoalType.AVERAGE_TIP_PERCENTAGE || goalType == TipGoalType.MEDIAN_TIP_PERCENTAGE) {
            if (targetValue.compareTo(BigDecimal.ZERO) < 0 || targetValue.compareTo(new BigDecimal("100.00")) > 0) {
                throw new IllegalArgumentException("Target percentage must be between 0 and 100");
            }
        }
        
        if (goalType == TipGoalType.TOTAL_TIP_AMOUNT && (currency == null || currency.isBlank())) {
            throw new IllegalArgumentException("TOTAL_TIP_AMOUNT goal requires a valid currency");
        }
    }
}
