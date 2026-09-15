package com.aitip.dto;

import java.time.LocalDate;

/**
 * Request DTO for tip analytics queries.
 *
 * <p><b>Filtering rules:</b></p>
 * <ul>
 *   <li>{@code period} is required.</li>
 *   <li>For {@code CUSTOM}, both {@code startDate} and {@code endDate} are required,
 *       and startDate must be &lt;= endDate.</li>
 *   <li>{@code restaurantName} and {@code serviceQuality} are optional in-memory filters.</li>
 * </ul>
 *
 * <p><b>Date semantics:</b> [startDate, endDate) — inclusive start, exclusive end.</p>
 */
public record AnalyticsRequest(
        AnalyticsPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        String restaurantName,
        ServiceQuality serviceQuality
) {}
