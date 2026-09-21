package com.aitip.controller;

import com.aitip.dto.ServiceQualityStatsResponse;
import com.aitip.service.ServiceQualityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for service quality statistics.
 *
 * <p><b>Purpose:</b> Exposes the deterministic service quality statistics
 * calculated by {@link ServiceQualityService} for the authenticated user.</p>
 *
 * <p><b>Security:</b>
 * All endpoints require JWT authentication. Spring Security's
 * {@code anyRequest().authenticated()} rule covers this controller
 * without requiring additional configuration.</p>
 *
 * <p><b>User isolation:</b>
 * The user email is resolved exclusively from the JWT token via
 * {@code Authentication.getName()}. No user identifier is accepted
 * from the request body or query parameters.</p>
 *
 * <p><b>No AI:</b>
 * This controller returns backend-calculated statistics only.
 * Groq is not involved.</p>
 */
@RestController
@RequestMapping("/api/service-quality")
public class ServiceQualityController {

    private final ServiceQualityService serviceQualityService;

    public ServiceQualityController(ServiceQualityService serviceQualityService) {
        this.serviceQualityService = serviceQualityService;
    }

    /**
     * Returns service quality statistics for the authenticated user.
     *
     * <p><b>Empty state:</b>
     * Returns {@code {"totalRatedTips": 0, ...}} when no tips are rated.
     * This is a valid response, not an error.</p>
     *
     * <p><b>Historical tips:</b>
     * Tips with {@code serviceQuality = null} are excluded from all statistics.
     * They are treated as NOT_RATED and do not affect counts or averages.</p>
     *
     * @param authentication JWT-injected authentication context
     * @return Service quality statistics for the caller's tips
     */
    @GetMapping("/summary")
    public ResponseEntity<ServiceQualityStatsResponse> getSummary(Authentication authentication) {
        ServiceQualityStatsResponse stats = serviceQualityService.getStats(authentication.getName());
        return ResponseEntity.ok(stats);
    }
}
