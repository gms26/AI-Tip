package com.aitip.controller;

import com.aitip.dto.PersonalizationResponse;
import com.aitip.service.PersonalizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for personalized tipping insights.
 *
 * <p>Secured by JWT filter. The user identity is extracted from the
 * {@link Authentication} object — never from a client-supplied userId.</p>
 */
@RestController
@RequestMapping("/api/personalization")
public class PersonalizationController {

    private final PersonalizationService personalizationService;

    public PersonalizationController(PersonalizationService personalizationService) {
        this.personalizationService = personalizationService;
    }

    /**
     * Returns the authenticated user's overall tipping pattern summary.
     */
    @GetMapping("/summary")
    public ResponseEntity<PersonalizationResponse> getSummary(Authentication authentication) {
        PersonalizationResponse response = personalizationService.getSummary(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Returns restaurant-specific personalization for the authenticated user.
     *
     * @param name The restaurant name to look up (case-insensitive)
     */
    @GetMapping("/restaurant")
    public ResponseEntity<PersonalizationResponse> getRestaurantPersonalization(
            @RequestParam String name,
            Authentication authentication) {
        PersonalizationResponse response = personalizationService.getRestaurantPersonalization(
                authentication.getName(), name);
        return ResponseEntity.ok(response);
    }
}
