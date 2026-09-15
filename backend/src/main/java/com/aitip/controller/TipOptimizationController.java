package com.aitip.controller;

import com.aitip.dto.TipOptimizationRequest;
import com.aitip.dto.TipOptimizationResponse;
import com.aitip.service.TipOptimizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tip optimization analysis.
 *
 * <p>All endpoints require JWT authentication.
 * User identity is resolved from the JWT token, never from the request body.</p>
 *
 * <p><b>Security:</b> No userId is accepted from the frontend.
 * {@code Authentication.getName()} determines the authenticated user.</p>
 */
@RestController
@RequestMapping("/api/tip-optimization")
public class TipOptimizationController {

    private final TipOptimizationService tipOptimizationService;

    public TipOptimizationController(TipOptimizationService tipOptimizationService) {
        this.tipOptimizationService = tipOptimizationService;
    }

    /**
     * Calculates tip optimization insights for the authenticated user.
     *
     * @param authentication JWT-based authentication
     * @param request        optimization parameters
     * @return deterministic optimization response
     */
    @PostMapping
    public ResponseEntity<TipOptimizationResponse> optimize(
            Authentication authentication,
            @Valid @RequestBody TipOptimizationRequest request) {

        TipOptimizationResponse response = tipOptimizationService.optimize(
                authentication.getName(), request);
        return ResponseEntity.ok(response);
    }
}
