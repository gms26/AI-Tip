package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.util.CurrencyValidationUtil;
import com.aitip.util.TipCalculationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic explainability service for Smart Tip Decisions (Day 33).
 */
@Service
public class SmartTipDecisionMemoryService {

    private static final Logger log = LoggerFactory.getLogger(SmartTipDecisionMemoryService.class);

    private final TipRecommendationFeedbackRepository feedbackRepository;
    private final SmartTipAdaptationService adaptationService;
    private final UserService userService;

    public SmartTipDecisionMemoryService(TipRecommendationFeedbackRepository feedbackRepository,
                                         SmartTipAdaptationService adaptationService,
                                         UserService userService) {
        this.feedbackRepository = feedbackRepository;
        this.adaptationService = adaptationService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public SmartTipDecisionMemoryResponse getDecisionMemory(String email, String currency) {
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);
        User user = userService.getUserByEmail(email);

        SmartTipAdaptationResult adaptation = adaptationService.getAdaptation(user, normalizedCurrency);

        BigDecimal min = feedbackRepository.findMinChosenPercentageByUserAndCurrency(user, normalizedCurrency);
        BigDecimal max = feedbackRepository.findMaxChosenPercentageByUserAndCurrency(user, normalizedCurrency);
        BigDecimal avg = feedbackRepository.findAverageChosenPercentageByUserAndCurrency(user, normalizedCurrency);
        
        // Let's compute median manually since we don't have a DB aggregate for it in all DBs. 
        // For median, we could reuse findTop5... wait, median is over ALL records. 
        // We can just load them or skip median if null. Since median is in the spec, we should calculate it. 
        // To avoid loading all, let's just do an order by query or just fetch chosen values. 
        // Actually, we can fetch all records for median via existing repo method `findAllByUserAndCurrencyOrderByCreatedAtAsc`.
        List<TipRecommendationFeedback> allFeedback = feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(user, normalizedCurrency);
        List<BigDecimal> chosenPcts = allFeedback.stream()
                .map(TipRecommendationFeedback::getChosenTipPercentage)
                .sorted()
                .toList();
                
        BigDecimal median = chosenPcts.isEmpty() ? null : TipCalculationUtil.calculateMedian(chosenPcts);

        SmartTipDecisionMemoryResponse.CommonChosenRange range = new SmartTipDecisionMemoryResponse.CommonChosenRange(
                min, max, median, avg
        );

        List<TipRecommendationFeedback> recentEntities = feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(user, normalizedCurrency);
        List<SmartTipRecentDecision> recentDecisions = recentEntities.stream()
                .map(f -> new SmartTipRecentDecision(
                        f.getCreatedAt(),
                        f.getCurrency(),
                        f.getRestaurantName(),
                        f.getSuggestedTipPercentage(),
                        f.getChosenTipPercentage(),
                        f.getDifferencePercentagePoints(),
                        f.getFeedbackType()
                ))
                .toList();

        return new SmartTipDecisionMemoryResponse(
                normalizedCurrency,
                adaptation.feedbackCount(),
                adaptation.acceptedCount(),
                adaptation.modifiedCount(),
                adaptation.customCount(),
                adaptation.averageDifferencePercentagePoints(),
                adaptation.direction(),
                calculateConfidence(adaptation.feedbackCount()),
                range,
                recentDecisions
        );
    }

    public SmartTipDecisionExplanation buildDecisionExplanation(SmartTipResponse response, SmartTipAdaptationResult adaptation, boolean personalizationEnabled) {
        if (response == null) {
            return null;
        }

        List<SmartTipDecisionFactor> factors = new ArrayList<>();

        if (adaptation != null && adaptation.adaptationApplied() && adaptation.adaptationAdjustment() != null) {
            String sign = adaptation.adaptationAdjustment().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            factors.add(new SmartTipDecisionFactor(
                    SmartTipDecisionFactorType.USER_FEEDBACK,
                    "Personalized Adjustment",
                    "Based on your recent tip choices.",
                    sign + adaptation.adaptationAdjustment().toPlainString() + " pp",
                    adaptation.adaptationAdjustment()
            ));
        }

        if (response.restaurantTipCount() != null && response.restaurantTipCount() > 0 && response.restaurantMedianTipPercentage() != null) {
            factors.add(new SmartTipDecisionFactor(
                    SmartTipDecisionFactorType.RESTAURANT_HISTORY,
                    "Restaurant History",
                    String.format("Based on %d previous visits here.", response.restaurantTipCount()),
                    response.restaurantMedianTipPercentage().toPlainString() + "%",
                    response.restaurantMedianTipPercentage()
            ));
        }

        if (response.optimizedMinimumPercentage() != null && response.optimizedMaximumPercentage() != null) {
            factors.add(new SmartTipDecisionFactor(
                    SmartTipDecisionFactorType.OPTIMIZATION_RANGE,
                    "Optimized Range",
                    "Recommended range for your normal behavior.",
                    response.optimizedMinimumPercentage().toPlainString() + "% – " + response.optimizedMaximumPercentage().toPlainString() + "%",
                    response.optimizedMinimumPercentage() // Using min as standard numeric value for this factor
            ));
        }

        if (response.historicalMedianTipPercentage() != null) {
            factors.add(new SmartTipDecisionFactor(
                    SmartTipDecisionFactorType.HISTORICAL_BEHAVIOR,
                    "Historical Median",
                    "Your typical baseline across all visits.",
                    response.historicalMedianTipPercentage().toPlainString() + "%",
                    response.historicalMedianTipPercentage()
            ));
        }

        if (response.recentDirection() != null && response.recentDirection() != TipEvolutionDirection.INSUFFICIENT_DATA) {
            String directionStr = response.recentDirection() == TipEvolutionDirection.MORE_GENEROUS ? "More Generous" : 
                                  response.recentDirection() == TipEvolutionDirection.MORE_CONSERVATIVE ? "More Conservative" : "Stable";
            factors.add(new SmartTipDecisionFactor(
                    SmartTipDecisionFactorType.RECENT_TREND,
                    "Recent Trend",
                    "Your tipping trend over the last 6 months.",
                    directionStr,
                    null
            ));
        }

        if (response.budgetStatus() != null) {
            factors.add(new SmartTipDecisionFactor(
                    SmartTipDecisionFactorType.BUDGET,
                    "Budget Status",
                    "Your current monthly budget standing.",
                    response.budgetStatus().name(),
                    response.budgetUsagePercentage()
            ));
        }

        if (response.serviceQualityTipCount() != null && response.serviceQualityTipCount() > 0 && response.serviceQualityAverageTipPercentage() != null) {
            factors.add(new SmartTipDecisionFactor(
                    SmartTipDecisionFactorType.SERVICE_QUALITY,
                    "Service Quality",
                    "How you tip for this level of service.",
                    response.serviceQualityAverageTipPercentage().toPlainString() + "%",
                    response.serviceQualityAverageTipPercentage()
            ));
        }
        
        factors.sort((a, b) -> Integer.compare(a.type().ordinal(), b.type().ordinal()));

        String summary;
        if (!personalizationEnabled) {
            summary = "Personalization is turned off, so your previous feedback was not used to adjust this recommendation.";
        } else if (factors.isEmpty()) {
            summary = "General tip options. Record tips to receive personalized suggestions.";
        } else {
            summary = "These factors influenced your personalized recommendation.";
        }

        BigDecimal baselinePercentage = response.baselinePrimarySuggestion() != null ? response.baselinePrimarySuggestion().tipPercentage() : 
                                        (response.primarySuggestion() != null ? response.primarySuggestion().tipPercentage() : null);
        BigDecimal adaptedPercentage = response.primarySuggestion() != null ? response.primarySuggestion().tipPercentage() : null;

        boolean adaptationApplied = adaptation != null && adaptation.adaptationApplied();
        BigDecimal adaptationAdjustment = adaptation != null ? adaptation.adaptationAdjustment() : null;
        String confidence = adaptation != null ? calculateConfidence(adaptation.feedbackCount()) : "LOW";

        return new SmartTipDecisionExplanation(
                summary,
                factors,
                baselinePercentage,
                adaptedPercentage,
                adaptationApplied,
                adaptationAdjustment,
                confidence
        );
    }

    private String calculateConfidence(long feedbackCount) {
        if (feedbackCount < 5) {
            return "LOW";
        } else if (feedbackCount <= 9) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
}
