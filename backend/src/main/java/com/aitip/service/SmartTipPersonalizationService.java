package com.aitip.service;

import com.aitip.dto.PersonalizationResetResponse;
import com.aitip.dto.PersonalizationSettingsResponse;
import com.aitip.entity.TipPersonalizationPreference;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.enums.PersonalizationStatus;
import com.aitip.repository.TipPersonalizationPreferenceRepository;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.util.CurrencyValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Personalization control service for Smart Tip recommendations (Day 36).
 *
 * <p>Provides explicit user control over the feedback-based adaptation layer:
 * <ul>
 *   <li>View personalization settings and learning state</li>
 *   <li>Enable/disable feedback-based adaptation</li>
 *   <li>Reset learned feedback for a specific currency</li>
 * </ul></p>
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Missing preference row = personalization enabled (backward compatible)</li>
 *   <li>Lazy preference creation (row created only when user explicitly toggles)</li>
 *   <li>Reset is user + currency scoped â€” never affects other users or currencies</li>
 *   <li>Reset does NOT delete tips, goals, budgets, achievements, or recommendation actions</li>
 * </ul></p>
 */
@Service
public class SmartTipPersonalizationService {

    private static final Logger log = LoggerFactory.getLogger(SmartTipPersonalizationService.class);

    private final TipPersonalizationPreferenceRepository preferenceRepository;
    private final TipRecommendationFeedbackRepository feedbackRepository;
    private final UserService userService;

    public SmartTipPersonalizationService(TipPersonalizationPreferenceRepository preferenceRepository,
                                          TipRecommendationFeedbackRepository feedbackRepository,
                                          UserService userService) {
        this.preferenceRepository = preferenceRepository;
        this.feedbackRepository = feedbackRepository;
        this.userService = userService;
    }

    /**
     * Returns the personalization settings for the authenticated user and currency.
     *
     * @param email    the authenticated user's email
     * @param currency ISO 4217 currency code
     * @return settings response with current state, feedback count, and learning date
     */
    @Transactional(readOnly = true)
    public PersonalizationSettingsResponse getSettings(String email, String currency) {
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);
        User user = userService.getUserByEmail(email);

        boolean enabled = isPersonalizationEnabled(user, normalizedCurrency);

        long feedbackCount = feedbackRepository.countByUserAndCurrency(user, normalizedCurrency);

        LocalDateTime lastLearningDate = null;
        if (feedbackCount > 0) {
            List<TipRecommendationFeedback> recent =
                    feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(user, normalizedCurrency);
            if (!recent.isEmpty()) {
                lastLearningDate = recent.get(0).getCreatedAt();
            }
        }

        boolean personalizationAvailable = feedbackCount >= SmartTipAdaptationService.MINIMUM_EVIDENCE_THRESHOLD;

        return new PersonalizationSettingsResponse(
                normalizedCurrency,
                enabled,
                enabled ? PersonalizationStatus.ENABLED : PersonalizationStatus.DISABLED,
                feedbackCount,
                lastLearningDate,
                personalizationAvailable
        );
    }

    /**
     * Updates the personalization setting for the authenticated user and currency.
     *
     * @param email    the authenticated user's email
     * @param currency ISO 4217 currency code
     * @param enabled  whether personalization should be enabled
     * @return updated settings response
     */
    @Transactional
    public PersonalizationSettingsResponse updateSettings(String email, String currency, boolean enabled) {
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);
        User user = userService.getUserByEmail(email);

        Optional<TipPersonalizationPreference> existing =
                preferenceRepository.findByUserAndCurrency(user, normalizedCurrency);

        if (existing.isPresent()) {
            TipPersonalizationPreference pref = existing.get();
            pref.setPersonalizationEnabled(enabled);
            preferenceRepository.save(pref);
        } else {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .user(user)
                    .currency(normalizedCurrency)
                    .personalizationEnabled(enabled)
                    .build();
            preferenceRepository.save(pref);
        }

        log.info("Updated personalization setting: user={}, currency={}, enabled={}", email, normalizedCurrency, enabled);

        return getSettings(email, normalizedCurrency);
    }

    /**
     * Resets (deletes) all learned feedback for the authenticated user and currency.
     *
     * <p>This operation is transactional and atomic. It deletes only
     * {@code TipRecommendationFeedback} records for the specified user + currency.
     * It does NOT delete tips, goals, budgets, achievements, or recommendation actions.</p>
     *
     * @param email    the authenticated user's email
     * @param currency ISO 4217 currency code
     * @return reset response with deleted count
     */
    @Transactional
    public PersonalizationResetResponse resetLearning(String email, String currency) {
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);
        User user = userService.getUserByEmail(email);

        long deletedCount = feedbackRepository.deleteAllByUserAndCurrency(user, normalizedCurrency);

        log.info("Reset personalization learning: user={}, currency={}, deletedCount={}",
                email, normalizedCurrency, deletedCount);

        return new PersonalizationResetResponse(true, normalizedCurrency, deletedCount);
    }

    /**
     * Checks whether personalization is enabled for the given user and currency.
     *
     * <p>Returns {@code true} if no preference row exists (backward compatible default).</p>
     *
     * @param user     the authenticated user entity
     * @param currency normalized ISO 4217 currency code
     * @return true if personalization is enabled or no preference exists
     */
    @Transactional(readOnly = true)
    public boolean isPersonalizationEnabled(User user, String currency) {
        if (user == null || currency == null) {
            return true; // safe default
        }
        return preferenceRepository.findByUserAndCurrency(user, currency)
                .map(TipPersonalizationPreference::isPersonalizationEnabled)
                .orElse(true); // default: enabled
    }
}
