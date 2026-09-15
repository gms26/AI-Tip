package com.aitip.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Advisory response from the Context-Aware Smart Tip Assistant (Day 31 & Day 32).
 *
 * <p>Contains factual context (historical baseline, optimization range, budget,
 * goals, restaurant and service quality patterns, recent direction), deterministic
 * tip suggestions, and Day 32 behavioral feedback adaptation facts.</p>
 */
public record SmartTipResponse(
        String currency,
        BigDecimal billAmount,
        BigDecimal historicalMedianTipPercentage,
        BigDecimal historicalAverageTipPercentage,
        BigDecimal optimizedMinimumPercentage,
        BigDecimal optimizedMaximumPercentage,
        TipBudgetStatus budgetStatus,
        BigDecimal budgetUsagePercentage,
        Boolean goalRelevant,
        TipEvolutionDirection recentDirection,
        Integer restaurantTipCount,
        BigDecimal restaurantMedianTipPercentage,
        BigDecimal restaurantAverageTipPercentage,
        Integer serviceQualityTipCount,
        BigDecimal serviceQualityAverageTipPercentage,
        List<SmartTipSuggestion> suggestions,
        SmartTipSuggestion primarySuggestion,
        String message,
        String aiExplanation,
        Long feedbackCount,
        SmartTipFeedbackDirection feedbackDirection,
        BigDecimal averageRecommendationDifference,
        Boolean adaptationApplied,
        BigDecimal adaptationAdjustment,
        String adaptationMessage,
        SmartTipSuggestion baselinePrimarySuggestion,
        SmartTipDecisionExplanation decisionExplanation,
        Boolean personalizationEnabled
) {
    public SmartTipResponse {
        if (billAmount != null) {
            billAmount = billAmount.setScale(2, RoundingMode.HALF_UP);
        }
        if (historicalMedianTipPercentage != null) {
            historicalMedianTipPercentage = historicalMedianTipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (historicalAverageTipPercentage != null) {
            historicalAverageTipPercentage = historicalAverageTipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (optimizedMinimumPercentage != null) {
            optimizedMinimumPercentage = optimizedMinimumPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (optimizedMaximumPercentage != null) {
            optimizedMaximumPercentage = optimizedMaximumPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (budgetUsagePercentage != null) {
            budgetUsagePercentage = budgetUsagePercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (restaurantMedianTipPercentage != null) {
            restaurantMedianTipPercentage = restaurantMedianTipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (restaurantAverageTipPercentage != null) {
            restaurantAverageTipPercentage = restaurantAverageTipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (serviceQualityAverageTipPercentage != null) {
            serviceQualityAverageTipPercentage = serviceQualityAverageTipPercentage.setScale(2, RoundingMode.HALF_UP);
        }
        if (averageRecommendationDifference != null) {
            averageRecommendationDifference = averageRecommendationDifference.setScale(2, RoundingMode.HALF_UP);
        }
        if (adaptationAdjustment != null) {
            adaptationAdjustment = adaptationAdjustment.setScale(2, RoundingMode.HALF_UP);
        }
        if (suggestions == null) {
            suggestions = List.of();
        }
        if (goalRelevant == null) {
            goalRelevant = false;
        }
        if (feedbackCount == null) {
            feedbackCount = 0L;
        }
        if (feedbackDirection == null) {
            feedbackDirection = SmartTipFeedbackDirection.INSUFFICIENT_DATA;
        }
        if (adaptationApplied == null) {
            adaptationApplied = false;
        }
        if (personalizationEnabled == null) {
            personalizationEnabled = true;
        }
    }

    public SmartTipResponse withAiExplanation(String newAiExplanation) {
        return new SmartTipResponse(
                currency,
                billAmount,
                historicalMedianTipPercentage,
                historicalAverageTipPercentage,
                optimizedMinimumPercentage,
                optimizedMaximumPercentage,
                budgetStatus,
                budgetUsagePercentage,
                goalRelevant,
                recentDirection,
                restaurantTipCount,
                restaurantMedianTipPercentage,
                restaurantAverageTipPercentage,
                serviceQualityTipCount,
                serviceQualityAverageTipPercentage,
                suggestions,
                primarySuggestion,
                message,
                newAiExplanation,
                feedbackCount,
                feedbackDirection,
                averageRecommendationDifference,
                adaptationApplied,
                adaptationAdjustment,
                adaptationMessage,
                baselinePrimarySuggestion,
                decisionExplanation,
                personalizationEnabled
        );
    }

    public SmartTipResponse withDecisionExplanation(SmartTipDecisionExplanation newDecisionExplanation) {
        return new SmartTipResponse(
                currency,
                billAmount,
                historicalMedianTipPercentage,
                historicalAverageTipPercentage,
                optimizedMinimumPercentage,
                optimizedMaximumPercentage,
                budgetStatus,
                budgetUsagePercentage,
                goalRelevant,
                recentDirection,
                restaurantTipCount,
                restaurantMedianTipPercentage,
                restaurantAverageTipPercentage,
                serviceQualityTipCount,
                serviceQualityAverageTipPercentage,
                suggestions,
                primarySuggestion,
                message,
                aiExplanation,
                feedbackCount,
                feedbackDirection,
                averageRecommendationDifference,
                adaptationApplied,
                adaptationAdjustment,
                adaptationMessage,
                baselinePrimarySuggestion,
                newDecisionExplanation,
                personalizationEnabled
        );
    }
}
