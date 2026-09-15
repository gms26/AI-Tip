package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Deterministic explanation factor contributing to a Smart Tip recommendation.
 */
public record SmartTipDecisionFactor(
        SmartTipDecisionFactorType type,
        String title,
        String description,
        String impact,
        BigDecimal value
) {
    public SmartTipDecisionFactor {
        if (value != null) {
            value = value.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
