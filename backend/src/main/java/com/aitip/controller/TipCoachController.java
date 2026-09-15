package com.aitip.controller;

import com.aitip.dto.TipCoachResponse;
import com.aitip.service.TipCoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tip-coach")
@RequiredArgsConstructor
public class TipCoachController {

    private final TipCoachService tipCoachService;

    @GetMapping
    public ResponseEntity<TipCoachResponse> getCoaching(
            Authentication authentication,
            @RequestParam(required = false) String currency) {
        
        String email = authentication.getName();
        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        return ResponseEntity.ok(response);
    }
}
