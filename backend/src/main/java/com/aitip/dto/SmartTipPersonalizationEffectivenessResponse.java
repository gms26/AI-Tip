package com.aitip.dto;

import com.aitip.enums.CalibrationStatus;
import com.aitip.enums.PersonalizationEffectiveness;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SmartTipPersonalizationEffectivenessResponse(
    String currency,
    boolean personalizationEnabled,
    long totalFeedback,
    long acceptedCount,
    long modifiedCount,
    long customCount,
    BigDecimal acceptanceRate,
    BigDecimal averageDifferencePercentagePoints,
    BigDecimal alignmentRate,
    SmartTipFeedbackDirection learningDirection,
    PersonalizationEffectiveness effectiveness,
    CalibrationStatus calibrationStatus,
    LocalDateTime lastFeedbackAt
) {}
