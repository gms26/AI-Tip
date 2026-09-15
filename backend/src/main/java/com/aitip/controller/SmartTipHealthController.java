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

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "deterministicRecommendations", true,
                "personalizationAvailable", true,
                "aiExplanationAvailable", true
        ));
    }
}
