package com.aitip.controller;

import com.aitip.dto.CreateTipGoalRequest;
import com.aitip.dto.TipGoalProgressResponse;
import com.aitip.dto.TipGoalResponse;
import com.aitip.dto.TipGoalSummaryResponse;
import com.aitip.service.TipGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tip-goals")
@RequiredArgsConstructor
public class TipGoalController {

    private final TipGoalService tipGoalService;

    @PostMapping
    public ResponseEntity<TipGoalResponse> createGoal(
            Authentication authentication,
            @Valid @RequestBody CreateTipGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tipGoalService.createGoal(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<TipGoalProgressResponse>> getGoals(Authentication authentication) {
        return ResponseEntity.ok(tipGoalService.getGoals(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipGoalResponse> getGoal(
            Authentication authentication,
            @PathVariable UUID id) {
        return ResponseEntity.ok(tipGoalService.getGoal(authentication.getName(), id));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<TipGoalProgressResponse> getGoalProgress(
            Authentication authentication,
            @PathVariable UUID id) {
        return ResponseEntity.ok(tipGoalService.getGoalProgress(authentication.getName(), id));
    }

    @GetMapping("/summary")
    public ResponseEntity<TipGoalSummaryResponse> getSummary(Authentication authentication) {
        return ResponseEntity.ok(tipGoalService.getSummary(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelGoal(
            Authentication authentication,
            @PathVariable UUID id) {
        tipGoalService.cancelGoal(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
