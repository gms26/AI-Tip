package com.aitip.dto;

import com.aitip.enums.PersonalizationStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for personalization settings state (Day 36).
 *
 * <p>Provides enough information for the UI to clearly explain
 * the personalization state and available learning data.</p>
 */
public record PersonalizationSettingsResponse(
        String currency,
        boolean enabled,
        PersonalizationStatus status,
        long feedbackCount,
        LocalDateTime lastLearningDate,
        boolean personalizationAvailable
) {}
