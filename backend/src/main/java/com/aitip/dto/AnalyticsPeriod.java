package com.aitip.dto;

/**
 * Defines the time periods available for tip analytics queries.
 *
 * <p><b>Design decision:</b> This is separate from {@link TaxPeriod}
 * because analytics supports additional periods (LAST_30_DAYS, LAST_90_DAYS, ALL_TIME)
 * that don't apply to tax tracking.</p>
 */
public enum AnalyticsPeriod {
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    CURRENT_YEAR,
    LAST_30_DAYS,
    LAST_90_DAYS,
    CUSTOM,
    ALL_TIME
}
