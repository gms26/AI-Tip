package com.aitip.controller;

import com.aitip.dto.TipForecastPeriod;
import com.aitip.dto.TipForecastResponse;
import com.aitip.service.TipForecastCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST controller for tip forecasting.
 *
 * <p>All endpoints require JWT authentication.
 * User identity is resolved from the JWT token, never from request parameters.</p>
 *
 * <p><b>Security:</b> No userId is accepted from the frontend.
 * {@code Authentication.getName()} determines the authenticated user.</p>
 *
 * <p><b>Read-only:</b> This endpoint never modifies tips, budgets,
 * or any other persisted data.</p>
 */
@RestController
@RequestMapping("/api/tip-forecast")
public class TipForecastController {

    private final TipForecastCalculationService forecastService;

    public TipForecastController(TipForecastCalculationService forecastService) {
        this.forecastService = forecastService;
    }

    /**
     * Returns a tip spending forecast for the authenticated user.
     *
     * @param authentication JWT-based authentication
     * @param currency       ISO 4217 currency code (required)
     * @param period         forecast period (required)
     * @param monthlyBudget  optional monthly budget for projection
     * @param lookbackDays   optional lookback window (default 90, range 7–365)
     * @return deterministic forecast response
     */
    @GetMapping
    public ResponseEntity<TipForecastResponse> forecast(
            Authentication authentication,
            @RequestParam String currency,
            @RequestParam TipForecastPeriod period,
            @RequestParam(required = false) BigDecimal monthlyBudget,
            @RequestParam(required = false) Integer lookbackDays) {

        TipForecastResponse response = forecastService.forecast(
                authentication.getName(), currency, period, monthlyBudget, lookbackDays);
        return ResponseEntity.ok(response);
    }
}
