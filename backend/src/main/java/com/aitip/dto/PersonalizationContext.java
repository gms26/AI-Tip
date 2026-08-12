package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Verified personalization context passed to the AI layer.
 *
 * <p><b>Architecture note:</b> This record carries backend-calculated
 * statistics into the prompt. Gemini must treat these as factual data,
 * NOT as something it should recalculate or invent.</p>
 */
public record PersonalizationContext(
        // Strongest eligible source and its confidence
        PersonalizationSource source,
        String confidence,

        // Overall stats
        Integer overallTipCount,
        BigDecimal overallMedianTipPercentage,
        BigDecimal overallAverageTipPercentage,

        // Restaurant stats
        String restaurantName,
        Integer restaurantVisitCount,
        BigDecimal restaurantMedianTipPercentage,
        BigDecimal restaurantAverageTipPercentage,
        BigDecimal lastTipPercentageAtRestaurant,
        LocalDateTime lastVisitDateAtRestaurant,
        ServiceQuality lastServiceQualityAtRestaurant,

        // Service Quality stats
        ServiceQuality serviceQuality,
        Integer serviceQualityCount,
        BigDecimal serviceQualityMedianTipPercentage,
        BigDecimal serviceQualityAverageTipPercentage,

        // Restaurant + Service Quality stats
        Integer restaurantAndServiceCount,
        BigDecimal restaurantAndServiceMedianTipPercentage,
        BigDecimal restaurantAndServiceAverageTipPercentage
) {
    public boolean hasPersonalizationData() {
        return source != PersonalizationSource.NONE;
    }

    public boolean hasRestaurantData() {
        return restaurantVisitCount != null && restaurantVisitCount > 0;
    }

    public boolean hasServiceQualityData() {
        return serviceQualityCount != null && serviceQualityCount > 0;
    }

    public boolean hasRestaurantAndServiceData() {
        return restaurantAndServiceCount != null && restaurantAndServiceCount > 0;
    }
}
