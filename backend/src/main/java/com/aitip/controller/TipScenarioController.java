package com.aitip.controller;

import com.aitip.dto.TipScenarioRequest;
import com.aitip.dto.TipScenarioResponse;
import com.aitip.service.TipScenarioCalculationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for what-if tip scenario planning.
 *
 * <p>All endpoints require JWT authentication.
 * User identity is resolved from the JWT token, never from request parameters.</p>
 *
 * <p><b>Read-only:</b> This endpoint never creates tips, modifies budgets,
 * triggers achievements, or persists any data.</p>
 *
 * <p><b>Security:</b> No userId is accepted from the frontend.
 * {@code Authentication.getName()} determines the authenticated user.</p>
 */
@RestController
@RequestMapping("/api/tip-scenarios")
public class TipScenarioController {

    private final TipScenarioCalculationService scenarioService;

    public TipScenarioController(TipScenarioCalculationService scenarioService) {
        this.scenarioService = scenarioService;
    }

    /**
     * Calculates hypothetical what-if tip scenarios for the authenticated user.
     *
     * @param authentication JWT-based authentication
     * @param request        validated scenario request
     * @return deterministic scenario response
     */
    @PostMapping
    public ResponseEntity<TipScenarioResponse> calculateScenarios(
            Authentication authentication,
            @RequestBody @Valid TipScenarioRequest request) {

        TipScenarioResponse response = scenarioService.calculate(
                authentication.getName(), request);
        return ResponseEntity.ok(response);
    }
}
