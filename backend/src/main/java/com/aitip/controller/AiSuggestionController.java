package com.aitip.controller;

import com.aitip.dto.AiSuggestionRequest;
import com.aitip.dto.AiSuggestionResponse;
import com.aitip.dto.PersonalizationContext;
import com.aitip.service.AiSuggestionService;
import com.aitip.service.PersonalizationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for AI interactions.
 * 
 * <p>Secured by default via JWT filter. Now injects {@link Authentication}
 * to look up the user's personalization context (Day 4).</p>
 *
 * <p><b>Graceful degradation:</b> If personalization fails (e.g., DB error),
 * the controller falls back to the Day 3 behavior (no personalization context)
 * rather than returning 503.</p>
 */
@RestController
@RequestMapping("/api/ai")
public class AiSuggestionController {

    private static final Logger log = LoggerFactory.getLogger(AiSuggestionController.class);

    private final AiSuggestionService aiSuggestionService;
    private final PersonalizationService personalizationService;

    public AiSuggestionController(AiSuggestionService aiSuggestionService,
                                  PersonalizationService personalizationService) {
        this.aiSuggestionService = aiSuggestionService;
        this.personalizationService = personalizationService;
    }

    /**
     * Retrieves an AI-driven tip recommendation based on service context
     * and the authenticated user's personalization data.
     * 
     * @param request The contextual details (bill, quality, type, etc)
     * @param authentication The authenticated user's security context
     * @return The AI recommendation and backend-calculated totals
     */
    @PostMapping("/suggest")
    public ResponseEntity<AiSuggestionResponse> getSuggestion(
            @Valid @RequestBody AiSuggestionRequest request,
            Authentication authentication) {
        
        // Attempt to build personalization context; fall back to null on failure
        PersonalizationContext context = null;
        try {
            context = personalizationService.buildContextForAi(
                    authentication.getName(), request.restaurantName(), request.serviceQuality());
        } catch (Exception e) {
            log.warn("Personalization lookup failed, falling back to Day 3 behavior: {}", e.getMessage());
        }

        AiSuggestionResponse response = aiSuggestionService.getRecommendation(request, context);
        return ResponseEntity.ok(response);
    }
}
