package com.aitip.controller;

import com.aitip.dto.TipTimingRequest;
import com.aitip.dto.TipTimingResponse;
import com.aitip.service.TipTimingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tip-timing")
@RequiredArgsConstructor
public class TipTimingController {

    private final TipTimingService tipTimingService;

    @PostMapping
    public ResponseEntity<TipTimingResponse> getTipTiming(
            @RequestBody TipTimingRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        TipTimingResponse response = tipTimingService.getTipTiming(email, request);
        return ResponseEntity.ok(response);
    }
}
