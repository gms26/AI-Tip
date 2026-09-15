package com.aitip.controller;

import com.aitip.dto.TipBudgetRequest;
import com.aitip.dto.TipBudgetResponse;
import com.aitip.dto.TipBudgetStatusResponse;
import com.aitip.service.TipBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for tip budget management.
 *
 * <p>All endpoints require JWT authentication.
 * User identity is resolved from the JWT token, never from the request body.</p>
 */
@RestController
@RequestMapping("/api/tip-budgets")
@RequiredArgsConstructor
public class TipBudgetController {

    private final TipBudgetService tipBudgetService;

    /**
     * Creates or updates a budget for the authenticated user.
     * If a budget for the given currency already exists, it is updated.
     */
    @PutMapping
    public ResponseEntity<TipBudgetResponse> createOrUpdateBudget(
            Authentication authentication,
            @Valid @RequestBody TipBudgetRequest request) {

        TipBudgetResponse response = tipBudgetService.createOrUpdateBudget(
                authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns all budgets for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<TipBudgetResponse>> getBudgets(Authentication authentication) {
        List<TipBudgetResponse> budgets = tipBudgetService.getBudgets(authentication.getName());
        return ResponseEntity.ok(budgets);
    }

    /**
     * Returns the deterministic budget status for a specific currency.
     */
    @GetMapping("/{currency}/status")
    public ResponseEntity<TipBudgetStatusResponse> getBudgetStatus(
            Authentication authentication,
            @PathVariable String currency) {

        TipBudgetStatusResponse status = tipBudgetService.getBudgetStatus(
                authentication.getName(), currency);
        return ResponseEntity.ok(status);
    }

    /**
     * Deletes the budget for a specific currency.
     */
    @DeleteMapping("/{currency}")
    public ResponseEntity<Void> deleteBudget(
            Authentication authentication,
            @PathVariable String currency) {

        tipBudgetService.deleteBudget(authentication.getName(), currency);
        return ResponseEntity.ok().build();
    }
}
