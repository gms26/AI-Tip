package com.aitip.controller;

import com.aitip.dto.GenerosityScoreResponse;
import com.aitip.service.GenerosityScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for providing the authenticated user's Generosity Score.
 * 
 * Uses Authentication.getName() to ensure strict user isolation.
 */
@RestController
@RequestMapping("/api/generosity")
@RequiredArgsConstructor
public class GenerosityScoreController {

    private final GenerosityScoreService generosityScoreService;

    @GetMapping("/score")
    public ResponseEntity<GenerosityScoreResponse> getGenerosityScore(Authentication authentication) {
        String email = authentication.getName();
        GenerosityScoreResponse response = generosityScoreService.getScore(email);
        return ResponseEntity.ok(response);
    }
}
