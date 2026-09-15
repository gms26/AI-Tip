package com.aitip.controller;

import com.aitip.dto.SmartTipLearningInsightsResponse;
import com.aitip.service.SmartTipLearningInsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Smart Tip Learning Insights endpoint (Day 35).
 *
 * <p>Exposes a read-only GET endpoint that returns deterministic learning insights
 * for the authenticated user and specified currency.</p>
 */
@RestController
@RequestMapping("/api/smart-tip")
public class SmartTipLearningInsightsController {

    private final SmartTipLearningInsightsService insightsService;

    public SmartTipLearningInsightsController(SmartTipLearningInsightsService insightsService) {
        this.insightsService = insightsService;
    }

    /**
     * Returns learning insights for the authenticated user's feedback in the given currency.
     *
     * @param authentication JWT authentication principal
     * @param currency       ISO 4217 currency code (required)
     * @return learning insights response with deterministic analysis
     */
    @GetMapping("/learning-insights")
    public ResponseEntity<SmartTipLearningInsightsResponse> getLearningInsights(
            Authentication authentication,
            @RequestParam String currency) {

        String email = authentication.getName();
        SmartTipLearningInsightsResponse response = insightsService.getInsights(email, currency);
        return ResponseEntity.ok(response);
    }
}
