package com.aitip.controller;

import com.aitip.dto.CreateTipRequest;
import com.aitip.dto.SplitRequest;
import com.aitip.dto.SplitResponse;
import com.aitip.dto.TipResponse;
import com.aitip.dto.UpdateServiceQualityRequest;
import com.aitip.service.TipCalculationService;
import com.aitip.service.TipService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for tip calculations and persistence.
 *
 * <p><b>Security:</b>
 * Secured by JWT filter. The {@link Authentication} object is
 * automatically injected by Spring Security, providing the
 * current user's email securely without trusting client payload.</p>
 */
@RestController
@RequestMapping("/api/tips")
public class TipController {

    private final TipService tipService;
    private final TipCalculationService tipCalculationService;

    public TipController(TipService tipService, TipCalculationService tipCalculationService) {
        this.tipService = tipService;
        this.tipCalculationService = tipCalculationService;
    }

    /**
     * Creates a new tip calculation and saves it.
     * serviceQuality is required as of Day 6.
     */
    @PostMapping
    public ResponseEntity<TipResponse> createTip(
            @Valid @RequestBody CreateTipRequest request,
            Authentication authentication) {
        
        TipResponse response = tipService.calculateAndSave(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists the current user's saved tips.
     * Includes pagination with a default sort by createdAt DESC.
     */
    @GetMapping
    public ResponseEntity<Page<TipResponse>> getUserTips(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 20) Pageable pageable,
            Authentication authentication) {
        
        Page<TipResponse> tips = tipService.getUserTips(authentication.getName(), pageable);
        return ResponseEntity.ok(tips);
    }

    /**
     * Retrieves a single tip by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TipResponse> getTipById(
            @PathVariable UUID id,
            Authentication authentication) {
        
        TipResponse response = tipService.getTipById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a saved tip.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTip(
            @PathVariable UUID id,
            Authentication authentication) {
        
        tipService.deleteTip(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Performs a stateless split calculation (not saved to DB).
     */
    @PostMapping("/split")
    public ResponseEntity<SplitResponse> splitTip(
            @Valid @RequestBody SplitRequest request) {
        
        SplitResponse response = tipCalculationService.calculateSplit(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the service quality rating of a specific tip.
     *
     * <p><b>Security:</b> JWT required. The service layer verifies that the
     * tip belongs to the authenticated user before updating. A 404 is returned
     * if the tip does not exist or belongs to another user (no enumeration leak).</p>
     *
     * @param id             The tip UUID to update
     * @param request        The new service quality value
     * @param authentication JWT-injected authentication context
     * @return Updated TipResponse
     */
    @PatchMapping("/{id}/service-quality")
    public ResponseEntity<TipResponse> updateServiceQuality(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceQualityRequest request,
            Authentication authentication) {

        TipResponse response = tipService.updateServiceQuality(
                id, authentication.getName(), request.serviceQuality());
        return ResponseEntity.ok(response);
    }
}
