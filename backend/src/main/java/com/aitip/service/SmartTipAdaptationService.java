package com.aitip.service;

import com.aitip.dto.SmartTipAdaptationResult;
import com.aitip.dto.SmartTipFeedbackDirection;
import com.aitip.dto.SmartTipFeedbackType;
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
import java.util.List;
import java.util.Objects;

/**
 * Deterministic behavioral analytics and adaptation service for Smart Tip recommendations (Day 32).
 *
 * <p>Principles:
 * <ul>
 *   <li>Deterministic, explainable mathematics using {@link BigDecimal} and {@link RoundingMode#HALF_UP}.</li>
 *   <li>Minimum evidence rule: Requires &gt;= 5 recommendation decisions before declaring direction or adapting.</li>
 *   <li>Adaptation threshold: Requires |averageDifference| &gt;= 2.00 percentage points.</li>
 *   <li>Bounded adjustment: Clamped strictly to [-3.00, +3.00] percentage points.</li>
 *   <li>Never replaces baseline recommendations — acts as a contextual overlay.</li>
 * </ul>
 * </p>
 */
@Service
public class SmartTipAdaptationService {

    private static final Logger log = LoggerFactory.getLogger(SmartTipAdaptationService.class);

    public static final int MINIMUM_EVIDENCE_THRESHOLD = 5;
    public static final BigDecimal ADAPTATION_THRESHOLD = new BigDecimal("2.00");
    public static final BigDecimal MAX_ADJUSTMENT = new BigDecimal("3.00");
    public static final BigDecimal MIN_ADJUSTMENT = new BigDecimal("-3.00");

    private final TipRecommendationFeedbackRepository feedbackRepository;

    public SmartTipAdaptationService(TipRecommendationFeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * Computes behavioral adaptation for the specified user and currency.
     *
     * @param user     the authenticated user
     * @param currency 3-letter ISO currency code
     * @return deterministic adaptation result
     */
    @Transactional(readOnly = true)
    public SmartTipAdaptationResult getAdaptation(User user, String currency) {
        if (user == null || currency == null) {
            return buildEmptyResult();
        }

        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);
        List<TipRecommendationFeedback> feedbackList =
                feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(user, normalizedCurrency);

        return analyze(feedbackList);
    }

    /**
     * Pure deterministic analysis function on a list of feedback records.
     *
     * @param feedbackList list of feedback records strictly isolated to a single user and currency
     * @return adaptation result
     */
    public SmartTipAdaptationResult analyze(List<TipRecommendationFeedback> feedbackList) {
        if (feedbackList == null || feedbackList.isEmpty()) {
            return buildEmptyResult();
        }

        long totalCount = feedbackList.size();
        long acceptedCount = feedbackList.stream().filter(f -> f.getFeedbackType() == SmartTipFeedbackType.ACCEPTED).count();
        long modifiedCount = feedbackList.stream().filter(f -> f.getFeedbackType() == SmartTipFeedbackType.MODIFIED).count();
        long customCount = feedbackList.stream().filter(f -> f.getFeedbackType() == SmartTipFeedbackType.CUSTOM).count();

        // Records with explicit recommendation difference
        List<BigDecimal> differences = feedbackList.stream()
                .map(TipRecommendationFeedback::getDifferencePercentagePoints)
                .filter(Objects::nonNull)
                .toList();

        int decisionCount = differences.size();

        if (decisionCount < MINIMUM_EVIDENCE_THRESHOLD) {
            BigDecimal partialAvg = decisionCount > 0 ? TipCalculationUtil.calculateMean(differences) : null;
            return new SmartTipAdaptationResult(
                    totalCount,
                    acceptedCount,
                    modifiedCount,
                    customCount,
                    decisionCount,
                    partialAvg,
                    SmartTipFeedbackDirection.INSUFFICIENT_DATA,
                    false,
                    null,
                    "Not enough feedback yet to identify a consistent preference."
            );
        }

        BigDecimal averageDifference = TipCalculationUtil.calculateMean(differences);

        SmartTipFeedbackDirection direction;
        boolean adaptationApplied;
        BigDecimal adjustment;
        String message;

        if (averageDifference.compareTo(ADAPTATION_THRESHOLD) >= 0) {
            direction = SmartTipFeedbackDirection.PREFERS_HIGHER;
            adaptationApplied = true;
            adjustment = averageDifference.min(MAX_ADJUSTMENT).setScale(2, RoundingMode.HALF_UP);
            message = String.format(
                    "Based on %d previous decisions, you usually choose around %s percentage points above the suggested tip.",
                    decisionCount,
                    averageDifference.abs().setScale(1, RoundingMode.HALF_UP).toPlainString()
            );
        } else if (averageDifference.compareTo(ADAPTATION_THRESHOLD.negate()) <= 0) {
            direction = SmartTipFeedbackDirection.PREFERS_LOWER;
            adaptationApplied = true;
            adjustment = averageDifference.max(MIN_ADJUSTMENT).setScale(2, RoundingMode.HALF_UP);
            message = String.format(
                    "Based on %d previous decisions, you usually choose around %s percentage points below the suggested tip.",
                    decisionCount,
                    averageDifference.abs().setScale(1, RoundingMode.HALF_UP).toPlainString()
            );
        } else {
            direction = SmartTipFeedbackDirection.ALIGNED;
            adaptationApplied = false;
            adjustment = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            message = "Your recent choices are generally close to the assistant's suggestions.";
        }

        return new SmartTipAdaptationResult(
                totalCount,
                acceptedCount,
                modifiedCount,
                customCount,
                decisionCount,
                averageDifference,
                direction,
                adaptationApplied,
                adjustment,
                message
        );
    }

    private SmartTipAdaptationResult buildEmptyResult() {
        return new SmartTipAdaptationResult(
                0,
                0,
                0,
                0,
                0,
                null,
                SmartTipFeedbackDirection.INSUFFICIENT_DATA,
                false,
                null,
                "Not enough feedback yet to identify a consistent preference."
        );
    }
}
