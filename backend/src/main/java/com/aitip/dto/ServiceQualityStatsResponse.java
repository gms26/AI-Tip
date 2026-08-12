package com.aitip.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response DTO for service quality statistics summary.
 *
 * <p><b>Purpose:</b> Returns deterministic, backend-calculated statistics
 * about the authenticated user's service quality ratings. No AI involved.
 * All values are derived from explicit user ratings stored in the DB.</p>
 *
 * <p><b>Empty state:</b>
 * When no tips have been rated, {@code totalRatedTips} is 0 and all other
 * fields are 0 or null. This is not an error; clients should display
 * "No service ratings yet." in the UI.</p>
 *
 * <p><b>Historical NULL handling:</b>
 * Tips with {@code service_quality = NULL} are excluded from all counts
 * and averages. They are treated as NOT_RATED and do NOT influence statistics.</p>
 *
 * <p><b>mostCommon tie-breaking rule:</b>
 * When two qualities share the highest count, the higher quality wins.
 * Fixed enum order: POOR &lt; AVERAGE &lt; GOOD &lt; EXCELLENT.
 * Example: GOOD=2, EXCELLENT=2 → mostCommon = EXCELLENT.</p>
 *
 * <p><b>averageTipPercentageByQuality decision:</b>
 * Only qualities with at least one rated tip are included in the map.
 * Qualities with zero tips are absent from the map (not present as 0.0).
 * This keeps the response clean and avoids confusion between "0% average"
 * and "no data available".</p>
 *
 * @param totalRatedTips              Total tips with a non-null service quality
 * @param poorCount                   Count of POOR-rated tips
 * @param averageCount                Count of AVERAGE-rated tips
 * @param goodCount                   Count of GOOD-rated tips
 * @param excellentCount              Count of EXCELLENT-rated tips
 * @param mostCommon                  The most common quality (null if no rated tips)
 * @param averageTipPercentageByQuality Map of quality → average tip %, scale=2, HALF_UP
 */
public record ServiceQualityStatsResponse(
        int totalRatedTips,
        int poorCount,
        int averageCount,
        int goodCount,
        int excellentCount,
        ServiceQuality mostCommon,
        Map<ServiceQuality, BigDecimal> averageTipPercentageByQuality
) {
}
