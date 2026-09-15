package com.aitip.controller;

import com.aitip.dto.AnalyticsRequest;
import com.aitip.dto.analytics.TipAnalyticsResponse;
import com.aitip.service.TipAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tip analytics.
 *
 * <p><b>Security:</b> User identity is obtained exclusively from
 * {@link Authentication#getName()}, never from the request body.
 * This prevents any user from accessing another user's analytics.</p>
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class TipAnalyticsController {

    private final TipAnalyticsService tipAnalyticsService;

    @PostMapping("/tips")
    public ResponseEntity<TipAnalyticsResponse> getAnalytics(
            @RequestBody AnalyticsRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(email, request);
        return ResponseEntity.ok(response);
    }
}
