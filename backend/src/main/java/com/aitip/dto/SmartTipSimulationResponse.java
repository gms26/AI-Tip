package com.aitip.dto;

public record SmartTipSimulationResponse(
        String currency,
        SmartTipSimulationState current,
        SmartTipSimulationState simulated,
        SmartTipSimulationDifference difference,
        String budgetImpact,
        String goalImpact,
        String explanation,
        String aiExplanation
) {
}
