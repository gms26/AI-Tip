package com.aitip.dto;

/**
 * Response DTO for the personalization reset operation (Day 36).
 *
 * <p>Clearly indicates success, affected currency, and how many
 * feedback records were removed.</p>
 */
public record PersonalizationResetResponse(
        boolean success,
        String currency,
        long deletedFeedbackCount
) {}
