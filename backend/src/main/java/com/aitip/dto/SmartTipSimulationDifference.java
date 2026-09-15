package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record SmartTipSimulationDifference(
        BigDecimal tipAmountDifference,
        BigDecimal totalAmountDifference,
        BigDecimal percentagePointDifference
) {
    public SmartTipSimulationDifference {
        if (tipAmountDifference != null) {
            tipAmountDifference = tipAmountDifference.setScale(2, RoundingMode.HALF_UP);
        }
        if (totalAmountDifference != null) {
            totalAmountDifference = totalAmountDifference.setScale(2, RoundingMode.HALF_UP);
        }
        if (percentagePointDifference != null) {
            percentagePointDifference = percentagePointDifference.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
