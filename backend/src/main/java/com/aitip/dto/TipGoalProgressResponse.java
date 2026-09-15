package com.aitip.dto;

import com.aitip.entity.TipGoalPeriod;
import com.aitip.entity.TipGoalStatus;
import com.aitip.entity.TipGoalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TipGoalProgressResponse(
        UUID id,
        TipGoalType goalType,
        BigDecimal targetValue,
        BigDecimal currentValue,
        BigDecimal progressPercentage,
        BigDecimal remainingValue,
        TipGoalStatus status,
        int sampleSize,
        String confidence,
        String message,
        
        // Context fields
        String currency,
        TipGoalPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        String restaurantName,
        ServiceQuality serviceQuality
) {}
