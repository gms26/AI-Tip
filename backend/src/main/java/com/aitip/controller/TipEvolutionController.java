package com.aitip.controller;

import com.aitip.dto.TipEvolutionPeriod;
import com.aitip.dto.TipEvolutionResponse;
import com.aitip.service.GeminiService;
import com.aitip.service.TipEvolutionPromptBuilder;
import com.aitip.service.TipEvolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tip-evolution")
public class TipEvolutionController {

    private static final Logger log = LoggerFactory.getLogger(TipEvolutionController.class);

    private final TipEvolutionService tipEvolutionService;
    private final TipEvolutionPromptBuilder promptBuilder;
    private final GeminiService geminiService;

    public TipEvolutionController(TipEvolutionService tipEvolutionService,
                                  TipEvolutionPromptBuilder promptBuilder,
                                  @Autowired(required = false) GeminiService geminiService) {
        this.tipEvolutionService = tipEvolutionService;
        this.promptBuilder = promptBuilder;
        this.geminiService = geminiService;
    }

    @GetMapping
    public ResponseEntity<TipEvolutionResponse> getEvolution(
            Authentication authentication,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false, defaultValue = "LAST_6_MONTHS") TipEvolutionPeriod period) {

        String email = authentication.getName();
        log.debug("Generating tip evolution for user={}, currency={}, period={}", email, currency, period);

        TipEvolutionResponse evolution = tipEvolutionService.getEvolution(email, currency, period);

        if (evolution.totalTipCount() > 0 && geminiService != null) {
            try {
                String prompt = promptBuilder.buildPrompt(evolution);
                java.util.concurrent.CompletableFuture<String> future = java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> geminiService.getRecommendation(prompt));
                String aiExplanation = future.get(3, java.util.concurrent.TimeUnit.SECONDS);
                if (aiExplanation != null && !aiExplanation.isBlank()) {
                    evolution = new TipEvolutionResponse(
                            evolution.generatedAt(),
                            evolution.period(),
                            evolution.currency(),
                            evolution.totalTipCount(),
                            evolution.activeMonths(),
                            evolution.overallDirection(),
                            evolution.currentMedianTipPercentage(),
                            evolution.currentAverageTipPercentage(),
                            evolution.historicalMedianTipPercentage(),
                            evolution.historicalAverageTipPercentage(),
                            evolution.currencyTimelines(),
                            evolution.serviceQualityEvolution(),
                            evolution.message(),
                            aiExplanation
                    );
                }
            } catch (Exception e) {
                log.warn("Gemini explanation failed or timed out for tip evolution, returning deterministic timeline: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(evolution);
    }
}
