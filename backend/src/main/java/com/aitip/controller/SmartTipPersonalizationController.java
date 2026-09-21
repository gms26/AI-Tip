package com.aitip.controller;

import com.aitip.dto.PersonalizationResetRequest;
import com.aitip.dto.PersonalizationResetResponse;
import com.aitip.dto.PersonalizationSettingsResponse;
import com.aitip.dto.PersonalizationUpdateRequest;
import com.aitip.dto.SmartTipPersonalizationEffectivenessResponse;
import com.aitip.service.SmartTipPersonalizationEffectivenessService;
import com.aitip.service.SmartTipPersonalizationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Smart Tip Personalization Controls (Day 36).
 *
 * <p>Exposes:
 * <ul>
 *   <li>{@code GET /api/smart-tip/personalization} â€” View personalization settings</li>
 *   <li>{@code PUT /api/smart-tip/personalization} â€” Enable or disable personalization</li>
 *   <li>{@code POST /api/smart-tip/personalization/reset} â€” Reset learned feedback</li>
 * </ul></p>
 *
 * <p>Strict Boundaries:
 * <ul>
 *   <li>User identity strictly derived from {@link Authentication#getName()}</li>
 *   <li>Never accepts userId from the client</li>
 *   <li>Currency isolation strictly enforced</li>
 * </ul></p>
 */
@RestController
@RequestMapping("/api/smart-tip/personalization")
public class SmartTipPersonalizationController {

    private static final Logger log = LoggerFactory.getLogger(SmartTipPersonalizationController.class);

    private final SmartTipPersonalizationService personalizationService;
    private final SmartTipPersonalizationEffectivenessService effectivenessService;

    public SmartTipPersonalizationController(
            SmartTipPersonalizationService personalizationService,
            SmartTipPersonalizationEffectivenessService effectivenessService) {
        this.personalizationService = personalizationService;
        this.effectivenessService = effectivenessService;
    }

    /**
     * Returns the personalization settings for the authenticated user and currency.
     */
    @GetMapping
    public ResponseEntity<PersonalizationSettingsResponse> getSettings(
            Authentication authentication,
            @RequestParam("currency") String currency) {

        String email = authentication.getName();
        log.debug("Fetching personalization settings for user={}, currency={}", email, currency);

        PersonalizationSettingsResponse response = personalizationService.getSettings(email, currency);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the personalization setting for the authenticated user and currency.
     */
    @PutMapping
    public ResponseEntity<PersonalizationSettingsResponse> updateSettings(
            Authentication authentication,
            @Valid @RequestBody PersonalizationUpdateRequest request) {

        String email = authentication.getName();
        log.debug("Updating personalization settings for user={}, currency={}, enabled={}",
                email, request.currency(), request.enabled());

        PersonalizationSettingsResponse response = personalizationService.updateSettings(
                email, request.currency(), request.enabled());
        return ResponseEntity.ok(response);
    }

    /**
     * Resets (deletes) all learned feedback for the authenticated user and currency.
     */
    @PostMapping("/reset")
    public ResponseEntity<PersonalizationResetResponse> resetLearning(
            Authentication authentication,
            @Valid @RequestBody PersonalizationResetRequest request) {

        String email = authentication.getName();
        log.debug("Resetting personalization learning for user={}, currency={}", email, request.currency());

        PersonalizationResetResponse response = personalizationService.resetLearning(email, request.currency());
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the personalization effectiveness metrics for the authenticated user and currency.
     */
    @GetMapping("/effectiveness")
    public ResponseEntity<SmartTipPersonalizationEffectivenessResponse> getEffectiveness(
            Authentication authentication,
            @RequestParam("currency") String currency) {

        String email = authentication.getName();
        log.debug("Fetching personalization effectiveness for user={}, currency={}", email, currency);

        SmartTipPersonalizationEffectivenessResponse response = effectivenessService.getEffectiveness(email, currency);
        return ResponseEntity.ok(response);
    }
}
