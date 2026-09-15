package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record SmartTipSimulationState(
        BigDecimal billAmount,
        BigDecimal tipPercentage,
        BigDecimal tipAmount,
        BigDecimal totalAmount
) {
    public SmartTipSimulationState {
        if (billAmount != null) {
            billAmount = billAmount.setScale(2, RoundingMode.HALF_UP);
        }
        if (tipPercentage != null) {
            tipPercentage = tipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (tipAmount != null) {
            tipAmount = tipAmount.setScale(2, RoundingMode.HALF_UP);
        }
        if (totalAmount != null) {
            totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
