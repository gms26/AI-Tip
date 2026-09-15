package com.aitip.controller;

import com.aitip.dto.CreateTipPoolRequest;
import com.aitip.dto.TipPoolResponse;
import com.aitip.service.TipPoolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tip-pools")
public class TipPoolController {

    private final TipPoolService tipPoolService;

    public TipPoolController(TipPoolService tipPoolService) {
        this.tipPoolService = tipPoolService;
    }

    @PostMapping
    public ResponseEntity<TipPoolResponse> createPool(
            @RequestBody CreateTipPoolRequest request,
            Authentication authentication) {
        TipPoolResponse response = tipPoolService.createPool(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TipPoolResponse>> getUserPools(Authentication authentication) {
        List<TipPoolResponse> responses = tipPoolService.getUserPools(authentication.getName());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipPoolResponse> getPoolById(
            @PathVariable UUID id,
            Authentication authentication) {
        TipPoolResponse response = tipPoolService.getPoolById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePool(
            @PathVariable UUID id,
            Authentication authentication) {
        tipPoolService.deleteDraftPool(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<TipPoolResponse> finalizePool(
            @PathVariable UUID id,
            Authentication authentication) {
        TipPoolResponse response = tipPoolService.finalizePool(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
