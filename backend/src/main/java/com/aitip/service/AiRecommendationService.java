package com.aitip.service;

import com.aitip.dto.TipRecommendationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);

    private final GeminiService geminiService;
    private final TipRecommendationPromptBuilder promptBuilder;
    private final TipProfilePromptBuilder profilePromptBuilder;

    public AiRecommendationService(GeminiService geminiService, 
                                   TipRecommendationPromptBuilder promptBuilder,
                                   TipProfilePromptBuilder profilePromptBuilder) {
        this.geminiService = geminiService;
        this.promptBuilder = promptBuilder;
        this.profilePromptBuilder = profilePromptBuilder;
    }

    public String generateExplanation(TipRecommendationResponse response) {
        String prompt = promptBuilder.buildPrompt(response.recommendations(), response.summary());
        
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    () -> geminiService.getRecommendation(prompt));
            return future.get(3, TimeUnit.SECONDS); 
        } catch (Exception e) {
            log.warn("Gemini timeout/failure during recommendation explanation: {}", e.getMessage());
            return null;
        }
    }

    public String getProfileExplanation(com.aitip.dto.TipProfileResponse response) {
        String prompt = profilePromptBuilder.buildPrompt(response);
        
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    () -> geminiService.getRecommendation(prompt));
            return future.get(3, TimeUnit.SECONDS); 
        } catch (Exception e) {
            log.warn("Gemini timeout/failure during profile explanation: {}", e.getMessage());
            return null;
        }
    }
}
