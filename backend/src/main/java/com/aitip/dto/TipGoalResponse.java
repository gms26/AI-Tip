package com.aitip.dto;

import com.aitip.entity.TipGoalPeriod;
import com.aitip.entity.TipGoalStatus;
import com.aitip.entity.TipGoalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TipGoalResponse(
        UUID id,
        TipGoalType goalType,
        BigDecimal targetValue,
        String currency,
        TipGoalPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        String restaurantName,
        ServiceQuality serviceQuality,
        TipGoalStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
