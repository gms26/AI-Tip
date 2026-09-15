package com.aitip.dto;

/**
 * Forecast period options for tip spending projection.
 *
 * <ul>
 *   <li>{@code CURRENT_MONTH} — remaining days in the current calendar month</li>
 *   <li>{@code NEXT_30_DAYS} — 30 days from now</li>
 *   <li>{@code NEXT_3_MONTHS} — 90 days from now</li>
 * </ul>
 */
public enum TipForecastPeriod {
    CURRENT_MONTH,
    NEXT_30_DAYS,
    NEXT_3_MONTHS
}
