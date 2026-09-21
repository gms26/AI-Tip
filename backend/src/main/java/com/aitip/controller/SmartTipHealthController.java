package com.aitip.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health endpoint for the Smart Tip feature.
 *
 * <p>Returns the operational status of the Smart Tip system.</p>
 */
@RestController
@RequestMapping("/api/smart-tip")
public class SmartTipHealthController {
    private final String groqApiKey;

    public SmartTipHealthController(@org.springframework.beans.factory.annotation.Value("${app.groq.api-key}") String groqApiKey) {
        this.groqApiKey = groqApiKey;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        boolean isAiAvailable = groqApiKey != null && !groqApiKey.isBlank() && !groqApiKey.equals("dummy_key");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "deterministicRecommendations", true,
                "personalizationAvailable", true,
                "aiExplanationAvailable", isAiAvailable
        ));
    }
}
