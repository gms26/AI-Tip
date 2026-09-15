package com.aitip.controller;

import com.aitip.dto.TipProfileResponse;
import com.aitip.service.AiRecommendationService;
import com.aitip.service.TipProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tip-profile")
public class TipProfileController {

    private static final Logger log = LoggerFactory.getLogger(TipProfileController.class);

    private final TipProfileService tipProfileService;
    private final AiRecommendationService aiRecommendationService;

    public TipProfileController(TipProfileService tipProfileService,
                                AiRecommendationService aiRecommendationService) {
        this.tipProfileService = tipProfileService;
        this.aiRecommendationService = aiRecommendationService;
    }

    @GetMapping
    public ResponseEntity<TipProfileResponse> getProfile(
            Authentication authentication,
            @RequestParam(required = false) String currency) {

        String email = authentication.getName();
        log.debug("Generating tip profile for {}, currency filter: {}", email, currency);

        // Deterministic profile calculation
        TipProfileResponse profile = tipProfileService.getProfile(email, currency);

        // Optional Gemini Explanation (fails gracefully)
        if (profile.totalTipCount() > 0) {
            try {
                String aiExplanation = aiRecommendationService.getProfileExplanation(profile);
                if (aiExplanation != null) {
                    profile = new TipProfileResponse(
                            profile.generatedAt(),
                            profile.totalTipCount(),
                            profile.currencyProfiles(),
                            profile.overallBehaviorType(),
                            profile.overallTipStyle(),
                            profile.historicalMedianTipPercentage(),
                            profile.historicalAverageTipPercentage(),
                            profile.tipPercentageMin(),
                            profile.tipPercentageMax(),
                            profile.consistencyScore(),
                            profile.recentMedianTipPercentage(),
                            profile.recentTrend(),
                            profile.topRestaurants(),
                            profile.topServiceQualities(),
                            profile.message(),
                            aiExplanation
                    );
                }
            } catch (Exception e) {
                log.warn("Gemini explanation failed for tip profile, returning deterministic facts only: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(profile);
    }
}
