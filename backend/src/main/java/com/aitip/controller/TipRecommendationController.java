package com.aitip.controller;

import com.aitip.dto.TipRecommendationActionRequest;
import com.aitip.dto.TipRecommendationActionResponse;
import com.aitip.dto.TipRecommendationHistoryResponse;
import com.aitip.dto.TipRecommendationResponse;
import com.aitip.service.AiRecommendationService;
import com.aitip.service.TipRecommendationActionService;
import com.aitip.service.TipRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class TipRecommendationController {

    private final TipRecommendationService recommendationService;
    private final AiRecommendationService aiRecommendationService;
    private final TipRecommendationActionService tipRecommendationActionService;

    @GetMapping
    public ResponseEntity<TipRecommendationResponse> getRecommendations(
            Authentication authentication,
            @RequestParam(required = false) String currency) {
            
        String email = authentication.getName();
        TipRecommendationResponse baseResponse = recommendationService.getRecommendations(email, currency);
        
        // Only call AI if we have recommendations
        String aiExplanation = null;
        if (!baseResponse.recommendations().isEmpty()) {
            try {
                aiExplanation = aiRecommendationService.generateExplanation(baseResponse);
            } catch (Exception e) {
                // Graceful fallback if Gemini fails
            }
        }
        
        TipRecommendationResponse finalResponse = new TipRecommendationResponse(
                baseResponse.generatedAt(),
                baseResponse.recommendations(),
                baseResponse.summary(),
                aiExplanation
        );
        
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/{type}/action")
    public ResponseEntity<TipRecommendationActionResponse> performAction(
            Authentication authentication,
            @PathVariable String type,
            @RequestParam(required = false) String currency,
            @RequestBody TipRecommendationActionRequest request) {
        String email = authentication.getName();
        TipRecommendationActionResponse response = tipRecommendationActionService.performAction(email, type, currency, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<TipRecommendationHistoryResponse> getHistory(Authentication authentication) {
        String email = authentication.getName();
        TipRecommendationHistoryResponse response = tipRecommendationActionService.getHistory(email);
        return ResponseEntity.ok(response);
    }
}
