package com.aitip.dto;

public record TipEvolutionRequest(
        String currency,
        TipEvolutionPeriod period
) {
    public TipEvolutionRequest {
        if (period == null) {
            period = TipEvolutionPeriod.LAST_6_MONTHS;
        }
    }
}
