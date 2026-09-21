package com.aitip.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health Check Controller.
 *
 * <p><b>Purpose:</b> Public endpoint for monitoring service availability.
 * Used by load balancers, deployment pipelines, and uptime monitors.</p>
 *
 * <p><b>Why a dedicated controller?</b>
 * <ul>
 *   <li>Health checks are infrastructure concerns, not business logic</li>
 *   <li>Keeps the path simple (/health) without the /api prefix</li>
 *   <li>Public endpoint â€” no authentication required</li>
 * </ul></p>
 *
 * <p><b>Why not Spring Boot Actuator?</b>
 * Actuator is heavier (many endpoints, dependencies). For Day 1,
 * a simple custom endpoint is sufficient. Actuator can be added later.</p>
 */
@RestController
public class HealthController {

    /**
     * Health check endpoint.
     *
     * @return status and current server timestamp
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "AI Tip Assistant",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
