package com.aitip.dto;

/**
 * Indicates the highest-priority context layer with sufficient historical data
 * that is actually used for personalization.
 */
public enum PersonalizationSource {
    RESTAURANT_AND_SERVICE,
    RESTAURANT,
    SERVICE_QUALITY,
    OVERALL,
    NONE
}
