package com.aitip.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for the Smart Tip Learning Insights endpoint (Day 35).
 *
 * <p>Aggregates deterministic insight data from the user's feedback history for a given currency.
 * All numerical values are backend-calculated; Gemini only provides optional natural-language wording.</p>
 */
public record SmartTipLearningInsightsResponse(
        // --- Feedback Summary ---
        long totalFeedback,
        long acceptedCount,
        long modifiedCount,
        long customCount,

        // --- Decision Alignment ---
        int usableDecisionCount,
        BigDecimal averageDifferencePercentagePoints,
        SmartTipFeedbackDirection preferenceDirection,

        // --- Learning Strength ---
        LearningStrength learningStrength,

        // --- Personalization Effect ---
        PersonalizationEffect personalizationEffect,

        // --- Preference Summary (deterministic human-readable text) ---
        String preferenceSummary,

        // --- Adaptation Summary (reused from Day 32) ---
        boolean adaptationApplied,
        BigDecimal adaptationAdjustment,

        // --- Recent Decisions (latest 5) ---
        List<RecentDecisionDto> recentDecisions,

        // --- Optional AI Explanation ---
        String aiExplanation,

        // --- Day 36: Personalization Status ---
        Boolean personalizationEnabled
) {}
