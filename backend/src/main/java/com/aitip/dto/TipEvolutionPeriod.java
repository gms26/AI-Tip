package com.aitip.dto;

public enum TipEvolutionPeriod {
    LAST_3_MONTHS(3),
    LAST_6_MONTHS(6),
    LAST_12_MONTHS(12),
    ALL_TIME(Integer.MAX_VALUE);

    private final int months;

    TipEvolutionPeriod(int months) {
        this.months = months;
    }

    public int getMonths() {
        return months;
    }
}
