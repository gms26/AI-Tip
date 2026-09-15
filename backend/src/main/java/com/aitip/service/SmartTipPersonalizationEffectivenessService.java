package com.aitip.service;

import com.aitip.dto.SmartTipAdaptationResult;
import com.aitip.dto.SmartTipFeedbackDirection;
import com.aitip.dto.SmartTipPersonalizationEffectivenessResponse;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.enums.CalibrationStatus;
import com.aitip.enums.PersonalizationEffectiveness;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.util.CurrencyValidationUtil;
import com.aitip.util.TipCalculationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Service to measure and report the effectiveness and calibration of personalization (Day 37).
 *
 * <p>It uses deterministic logic to calculate how well recommendations match
 * the user's actual choices, providing metrics like acceptance rate, alignment rate,
 * and calibration status without requiring any AI interaction.</p>
 */
@Service
public class SmartTipPersonalizationEffectivenessService {

    private final TipRecommendationFeedbackRepository feedbackRepository;
    private final SmartTipPersonalizationService personalizationService;
    private final SmartTipAdaptationService adaptationService;
    private final UserService userService;

    public SmartTipPersonalizationEffectivenessService(
            TipRecommendationFeedbackRepository feedbackRepository,
            SmartTipPersonalizationService personalizationService,
            SmartTipAdaptationService adaptationService,
            UserService userService) {
        this.feedbackRepository = feedbackRepository;
        this.personalizationService = personalizationService;
        this.adaptationService = adaptationService;
        this.userService = userService;
    }

    /**
     * Computes personalization effectiveness metrics for the given user and currency.
     *
     * @param email    The user's email
     * @param currency The ISO 4217 currency code
     * @return Deterministic effectiveness metrics
     */
    @Transactional(readOnly = true)
    public SmartTipPersonalizationEffectivenessResponse getEffectiveness(String email, String currency) {
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);
        User user = userService.getUserByEmail(email);
        boolean isEnabled = personalizationService.isPersonalizationEnabled(user, normalizedCurrency);

        List<TipRecommendationFeedback> feedbackList = 
                feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(user, normalizedCurrency);

        if (feedbackList == null || feedbackList.isEmpty()) {
            return buildEmptyResponse(normalizedCurrency, isEnabled);
        }

        long totalFeedback = feedbackList.size();
        SmartTipAdaptationResult adaptationResult = adaptationService.analyze(feedbackList);

        BigDecimal acceptanceRate = BigDecimal.valueOf(adaptationResult.acceptedCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalFeedback), 2, RoundingMode.HALF_UP);

        List<BigDecimal> absDifferences = feedbackList.stream()
                .map(TipRecommendationFeedback::getDifferencePercentagePoints)
                .filter(Objects::nonNull)
                .map(BigDecimal::abs)
                .toList();

        BigDecimal averageAbsDiff = null;
        BigDecimal alignmentRate = null;
        CalibrationStatus calibrationStatus = CalibrationStatus.NOT_ENOUGH_DATA;

        if (!absDifferences.isEmpty()) {
            averageAbsDiff = TipCalculationUtil.calculateMean(absDifferences);
            
            long alignedCount = absDifferences.stream()
                    .filter(diff -> diff.compareTo(BigDecimal.ONE) <= 0)
                    .count();
            
            alignmentRate = BigDecimal.valueOf(alignedCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(absDifferences.size()), 2, RoundingMode.HALF_UP);
                    
            if (totalFeedback >= SmartTipAdaptationService.MINIMUM_EVIDENCE_THRESHOLD) {
                if (averageAbsDiff.compareTo(BigDecimal.ONE) <= 0) {
                    calibrationStatus = CalibrationStatus.WELL_CALIBRATED;
                } else if (averageAbsDiff.compareTo(new BigDecimal("3.00")) <= 0) {
                    calibrationStatus = CalibrationStatus.SLIGHTLY_OFF;
                } else {
                    calibrationStatus = CalibrationStatus.SIGNIFICANTLY_OFF;
                }
            }
        }

        PersonalizationEffectiveness effectiveness;
        if (totalFeedback < 5) {
            effectiveness = PersonalizationEffectiveness.INSUFFICIENT_DATA;
        } else if (totalFeedback < 10) {
            effectiveness = PersonalizationEffectiveness.EARLY_SIGNAL;
        } else {
            if (alignmentRate != null && alignmentRate.compareTo(new BigDecimal("80.00")) >= 0) {
                effectiveness = PersonalizationEffectiveness.STRONG_ALIGNMENT;
            } else if (alignmentRate != null && alignmentRate.compareTo(new BigDecimal("60.00")) >= 0) {
                effectiveness = PersonalizationEffectiveness.HELPFUL;
            } else {
                effectiveness = PersonalizationEffectiveness.LOW_ALIGNMENT;
            }
        }

        LocalDateTime lastFeedbackAt = feedbackList.get(feedbackList.size() - 1).getCreatedAt();

        return new SmartTipPersonalizationEffectivenessResponse(
                normalizedCurrency,
                isEnabled,
                totalFeedback,
                adaptationResult.acceptedCount(),
                adaptationResult.modifiedCount(),
                adaptationResult.customCount(),
                acceptanceRate,
                averageAbsDiff,
                alignmentRate,
                adaptationResult.direction(),
                effectiveness,
                calibrationStatus,
                lastFeedbackAt
        );
    }

    private SmartTipPersonalizationEffectivenessResponse buildEmptyResponse(String currency, boolean isEnabled) {
        return new SmartTipPersonalizationEffectivenessResponse(
                currency,
                isEnabled,
                0,
                0,
                0,
                0,
                null,
                null,
                null,
                SmartTipFeedbackDirection.INSUFFICIENT_DATA,
                PersonalizationEffectiveness.INSUFFICIENT_DATA,
                CalibrationStatus.NOT_ENOUGH_DATA,
                null
        );
    }
}
