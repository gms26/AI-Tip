package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.repository.UserRepository;
import com.aitip.util.CurrencyValidationUtil;
import com.aitip.util.TipCalculationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic learning-insights service for Smart Tip feedback analysis (Day 35).
 *
 * <p>Provides a read-only insight layer over the user's existing Day 32 feedback data.
 * All numerical values are computed deterministically with {@link BigDecimal}.
 * Gemini is used only for optional natural-language summaries.</p>
 *
 * <p>Key design constraints:
 * <ul>
 *   <li>No new database tables.</li>
 *   <li>No modification of existing records.</li>
 *   <li>No automatic feedback creation.</li>
 *   <li>Strict user and currency isolation.</li>
 * </ul></p>
 */
@Service
public class SmartTipLearningInsightsService {

    private static final Logger log = LoggerFactory.getLogger(SmartTipLearningInsightsService.class);

    /**
     * Minimum usable decisions required to calculate personalization effect (earlier/recent split).
     */
    static final int MIN_PERSONALIZATION_DECISIONS = 4;

    /**
     * Threshold in percentage points for determining IMPROVING vs DIVERGING vs STABLE.
     * If the absolute difference between earlier and recent average deviations exceeds this,
     * the trend is classified as IMPROVING or DIVERGING; otherwise STABLE.
     */
    static final BigDecimal PERSONALIZATION_THRESHOLD = new BigDecimal("0.50");

    private final TipRecommendationFeedbackRepository feedbackRepository;
    private final SmartTipAdaptationService adaptationService;
    private final UserRepository userRepository;
    private final GeminiService geminiService;
    private final SmartTipPersonalizationService personalizationService;

    public SmartTipLearningInsightsService(
            TipRecommendationFeedbackRepository feedbackRepository,
            SmartTipAdaptationService adaptationService,
            UserRepository userRepository,
            GeminiService geminiService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SmartTipPersonalizationService personalizationService) {
        this.feedbackRepository = feedbackRepository;
        this.adaptationService = adaptationService;
        this.userRepository = userRepository;
        this.geminiService = geminiService;
        this.personalizationService = personalizationService;
    }

    /**
     * Computes the full learning-insights response for the authenticated user and currency.
     *
     * @param email    the authenticated user's email
     * @param currency ISO 4217 currency code
     * @return a fully-populated insights response; never null
     */
    @Transactional(readOnly = true)
    public SmartTipLearningInsightsResponse getInsights(String email, String currency) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);

        // Fetch all feedback chronologically ascending
        List<TipRecommendationFeedback> allFeedback =
                feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(user, normalizedCurrency);

        // --- Feedback Summary ---
        long totalFeedback = allFeedback.size();
        long acceptedCount = allFeedback.stream()
                .filter(f -> f.getFeedbackType() == SmartTipFeedbackType.ACCEPTED).count();
        long modifiedCount = allFeedback.stream()
                .filter(f -> f.getFeedbackType() == SmartTipFeedbackType.MODIFIED).count();
        long customCount = allFeedback.stream()
                .filter(f -> f.getFeedbackType() == SmartTipFeedbackType.CUSTOM).count();

        // --- Usable decisions: those with non-null differencePercentagePoints ---
        List<TipRecommendationFeedback> usableDecisions = allFeedback.stream()
                .filter(f -> f.getDifferencePercentagePoints() != null)
                .toList();
        int usableDecisionCount = usableDecisions.size();

        List<BigDecimal> differences = usableDecisions.stream()
                .map(TipRecommendationFeedback::getDifferencePercentagePoints)
                .toList();

        // --- Average Difference ---
        BigDecimal averageDifference = usableDecisionCount > 0
                ? TipCalculationUtil.calculateMean(differences)
                : null;

        // --- Preference Direction (reuse Day 32 thresholds) ---
        SmartTipFeedbackDirection preferenceDirection = determineDirection(usableDecisionCount, averageDifference);

        // --- Learning Strength ---
        LearningStrength learningStrength = LearningStrength.fromCount(usableDecisionCount);

        // --- Personalization Effect ---
        PersonalizationEffect personalizationEffect = determinePersonalizationEffect(usableDecisions);

        // --- Preference Summary (deterministic text) ---
        String preferenceSummary = generatePreferenceSummary(
                usableDecisionCount, preferenceDirection, acceptedCount, totalFeedback);

        // --- Adaptation Summary (reuse Day 32) ---
        SmartTipAdaptationResult adaptation = adaptationService.getAdaptation(user, normalizedCurrency);

        // --- Recent Decisions (latest 5) ---
        List<RecentDecisionDto> recentDecisions = buildRecentDecisions(user, normalizedCurrency);

        // --- Optional AI Explanation ---
        String aiExplanation = generateAiExplanation(
                usableDecisionCount, averageDifference, preferenceDirection,
                learningStrength, personalizationEffect);

        // --- Day 36: Personalization Status ---
        boolean personalizationEnabled = personalizationService == null
                || personalizationService.isPersonalizationEnabled(user, normalizedCurrency);

        return new SmartTipLearningInsightsResponse(
                totalFeedback,
                acceptedCount,
                modifiedCount,
                customCount,
                usableDecisionCount,
                averageDifference,
                preferenceDirection,
                learningStrength,
                personalizationEffect,
                preferenceSummary,
                adaptation.adaptationApplied(),
                adaptation.adaptationAdjustment(),
                recentDecisions,
                aiExplanation,
                personalizationEnabled
        );
    }

    /**
     * Determines the preference direction using Day 32's established ±2.00 pp threshold.
     */
    SmartTipFeedbackDirection determineDirection(int usableCount, BigDecimal averageDifference) {
        if (usableCount < SmartTipAdaptationService.MINIMUM_EVIDENCE_THRESHOLD || averageDifference == null) {
            return SmartTipFeedbackDirection.INSUFFICIENT_DATA;
        }
        if (averageDifference.compareTo(SmartTipAdaptationService.ADAPTATION_THRESHOLD) >= 0) {
            return SmartTipFeedbackDirection.PREFERS_HIGHER;
        }
        if (averageDifference.compareTo(SmartTipAdaptationService.ADAPTATION_THRESHOLD.negate()) <= 0) {
            return SmartTipFeedbackDirection.PREFERS_LOWER;
        }
        return SmartTipFeedbackDirection.ALIGNED;
    }

    /**
     * Determines the personalization effect by comparing earlier-half and recent-half absolute deviations.
     *
     * <p>Requires at least {@link #MIN_PERSONALIZATION_DECISIONS} usable decisions to split meaningfully.
     * Uses a threshold of {@link #PERSONALIZATION_THRESHOLD} pp to classify the trend.</p>
     */
    PersonalizationEffect determinePersonalizationEffect(List<TipRecommendationFeedback> usableDecisions) {
        if (usableDecisions.size() < MIN_PERSONALIZATION_DECISIONS) {
            return PersonalizationEffect.INSUFFICIENT_DATA;
        }

        int mid = usableDecisions.size() / 2;
        List<TipRecommendationFeedback> earlierHalf = usableDecisions.subList(0, mid);
        List<TipRecommendationFeedback> recentHalf = usableDecisions.subList(mid, usableDecisions.size());

        BigDecimal earlierAbsAvg = calculateAbsoluteAverageDeviation(earlierHalf);
        BigDecimal recentAbsAvg = calculateAbsoluteAverageDeviation(recentHalf);

        // improvement = earlier average deviation - recent average deviation
        BigDecimal improvement = earlierAbsAvg.subtract(recentAbsAvg);

        if (improvement.compareTo(PERSONALIZATION_THRESHOLD) >= 0) {
            return PersonalizationEffect.IMPROVING;
        }
        if (improvement.compareTo(PERSONALIZATION_THRESHOLD.negate()) <= 0) {
            return PersonalizationEffect.DIVERGING;
        }
        return PersonalizationEffect.STABLE;
    }

    /**
     * Calculates the mean of absolute differencePercentagePoints for a subset of decisions.
     */
    BigDecimal calculateAbsoluteAverageDeviation(List<TipRecommendationFeedback> decisions) {
        if (decisions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> absValues = decisions.stream()
                .map(f -> f.getDifferencePercentagePoints().abs())
                .toList();
        return TipCalculationUtil.calculateMean(absValues);
    }

    /**
     * Generates a deterministic preference summary string.
     * No Gemini involvement.
     */
    String generatePreferenceSummary(int usableCount, SmartTipFeedbackDirection direction,
                                     long acceptedCount, long totalFeedback) {
        if (usableCount < SmartTipAdaptationService.MINIMUM_EVIDENCE_THRESHOLD) {
            return "Not enough decisions yet";
        }

        return switch (direction) {
            case ALIGNED -> {
                if (totalFeedback > 0 && acceptedCount * 2 >= totalFeedback) {
                    yield "Usually follows recommendations";
                }
                yield "Frequently adjusts recommendations";
            }
            case PREFERS_HIGHER -> "Often tips slightly higher than recommended";
            case PREFERS_LOWER -> "Often tips slightly lower than recommended";
            case INSUFFICIENT_DATA -> "Not enough decisions yet";
        };
    }

    /**
     * Builds the list of the latest 5 usable decisions.
     */
    private List<RecentDecisionDto> buildRecentDecisions(User user, String currency) {
        List<TipRecommendationFeedback> recent =
                feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(user, currency);
        if (recent == null || recent.isEmpty()) {
            return Collections.emptyList();
        }
        return recent.stream()
                .map(f -> new RecentDecisionDto(
                        f.getCreatedAt(),
                        f.getRestaurantName(),
                        f.getServiceQuality(),
                        f.getSuggestedTipPercentage(),
                        f.getChosenTipPercentage(),
                        f.getDifferencePercentagePoints(),
                        f.getFeedbackType(),
                        f.getRecommendationType()
                ))
                .toList();
    }

    /**
     * Optionally generates a friendly AI explanation from pre-calculated facts.
     * Returns null if Gemini is unavailable rather than failing the entire response.
     */
    private String generateAiExplanation(
            int usableDecisionCount,
            BigDecimal averageDifference,
            SmartTipFeedbackDirection direction,
            LearningStrength strength,
            PersonalizationEffect effect) {
        if (usableDecisionCount == 0) {
            return null;
        }
        try {
            java.util.concurrent.CompletableFuture<String> future = java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> geminiService.generateLearningInsightsExplanation(
                            usableDecisionCount, averageDifference, direction, strength, effect));
            return future.get(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Gemini explanation unavailable or timed out for learning insights: {}", e.getMessage());
            return null;
        }
    }
}
