package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.util.CurrencyValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Service for recording and summarizing Smart Tip recommendation feedback (Day 32).
 *
 * <p>Enforces strict deterministic feedback classification, financial arithmetic via {@link BigDecimal},
 * and strict user and currency isolation.</p>
 */
@Service
public class SmartTipFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(SmartTipFeedbackService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal MIN_VALID_BILL = new BigDecimal("0.01");

    private final TipRecommendationFeedbackRepository feedbackRepository;
    private final UserService userService;
    private final SmartTipAdaptationService adaptationService;
    private final SmartTipPersonalizationService personalizationService;
    private final Clock clock;

    @Autowired
    public SmartTipFeedbackService(TipRecommendationFeedbackRepository feedbackRepository,
                                   UserService userService,
                                   SmartTipAdaptationService adaptationService,
                                   @Autowired(required = false) SmartTipPersonalizationService personalizationService) {
        this(feedbackRepository, userService, adaptationService, personalizationService, Clock.systemDefaultZone());
    }

    public SmartTipFeedbackService(TipRecommendationFeedbackRepository feedbackRepository,
                                   UserService userService,
                                   SmartTipAdaptationService adaptationService,
                                   SmartTipPersonalizationService personalizationService,
                                   Clock clock) {
        this.feedbackRepository = feedbackRepository;
        this.userService = userService;
        this.adaptationService = adaptationService;
        this.personalizationService = personalizationService;
        this.clock = clock;
    }

    /**
     * Records explicit feedback for a saved tip decision.
     *
     * @param email   the authenticated user's email
     * @param request feedback payload
     * @return deterministic classification and difference result
     */
    @Transactional
    public SmartTipFeedbackResponse recordFeedback(String email, SmartTipFeedbackRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Feedback request cannot be null");
        }
        if (request.billAmount() == null || request.billAmount().compareTo(MIN_VALID_BILL) <= 0) {
            throw new IllegalArgumentException("Bill amount must be greater than 0.01");
        }

        String currency = CurrencyValidationUtil.normalizeAndValidate(request.currency());
        User user = userService.getUserByEmail(email);

        // Day 36: Do not record feedback when personalization is disabled
        if (personalizationService != null && !personalizationService.isPersonalizationEnabled(user, currency)) {
            log.debug("Skipping feedback recording: personalization disabled for user={}, currency={}", email, currency);
            return new SmartTipFeedbackResponse(
                    SmartTipFeedbackType.CUSTOM,
                    request.suggestedTipPercentage(),
                    request.chosenTipPercentage(),
                    null
            );
        }

        BigDecimal chosen = request.chosenTipPercentage();
        if (chosen == null || chosen.compareTo(BigDecimal.ZERO) < 0 || chosen.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Chosen tip percentage must be between 0 and 100");
        }
        chosen = chosen.setScale(2, RoundingMode.HALF_UP);

        BigDecimal suggested = request.suggestedTipPercentage();
        if (suggested != null) {
            if (suggested.compareTo(BigDecimal.ZERO) < 0 || suggested.compareTo(HUNDRED) > 0) {
                throw new IllegalArgumentException("Suggested tip percentage must be between 0 and 100");
            }
            suggested = suggested.setScale(2, RoundingMode.HALF_UP);
        }

        SmartTipFeedbackType feedbackType;
        BigDecimal difference;

        if (suggested != null) {
            if (chosen.compareTo(suggested) == 0) {
                feedbackType = SmartTipFeedbackType.ACCEPTED;
                difference = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                feedbackType = SmartTipFeedbackType.MODIFIED;
                difference = chosen.subtract(suggested).setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            feedbackType = SmartTipFeedbackType.CUSTOM;
            difference = null;
        }

        TipRecommendationFeedback entity = TipRecommendationFeedback.builder()
                .user(user)
                .currency(currency)
                .restaurantName(request.restaurantName())
                .serviceQuality(request.serviceQuality())
                .billAmount(request.billAmount().setScale(2, RoundingMode.HALF_UP))
                .suggestedTipPercentage(suggested)
                .chosenTipPercentage(chosen)
                .feedbackType(feedbackType)
                .recommendationType(request.suggestedRecommendationType())
                .differencePercentagePoints(difference)
                .createdAt(LocalDateTime.now(clock))
                .build();

        feedbackRepository.save(entity);
        log.info("Recorded smart tip feedback: user={}, currency={}, type={}, suggested={}, chosen={}, diff={}",
                email, currency, feedbackType, suggested, chosen, difference);

        return new SmartTipFeedbackResponse(
                feedbackType,
                suggested,
                chosen,
                difference
        );
    }

    /**
     * Retrieves the behavioral summary for the authenticated user and specified currency.
     *
     * @param email    the authenticated user's email
     * @param currency 3-letter ISO currency code
     * @return behavioral feedback summary
     */
    @Transactional(readOnly = true)
    public SmartTipFeedbackSummaryResponse getSummary(String email, String currency) {
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);
        User user = userService.getUserByEmail(email);

        SmartTipAdaptationResult adaptation = adaptationService.getAdaptation(user, normalizedCurrency);

        return new SmartTipFeedbackSummaryResponse(
                normalizedCurrency,
                adaptation.feedbackCount(),
                adaptation.acceptedCount(),
                adaptation.modifiedCount(),
                adaptation.customCount(),
                adaptation.averageDifferencePercentagePoints(),
                adaptation.direction(),
                adaptation.adaptationApplied(),
                adaptation.adaptationAdjustment()
        );
    }
}
