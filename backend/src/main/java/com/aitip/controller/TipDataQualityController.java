package com.aitip.controller;

import com.aitip.dto.TipAnomalySeverity;
import com.aitip.dto.TipAnomalyType;
import com.aitip.dto.TipDataQualityResponse;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.UserRepository;
import com.aitip.service.TipDataQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tips/data-quality")
@RequiredArgsConstructor
public class TipDataQualityController {

    private final TipDataQualityService tipDataQualityService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<TipDataQualityResponse> getDataQuality(
            Authentication authentication,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) TipAnomalySeverity severity,
            @RequestParam(required = false) TipAnomalyType anomalyType
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TipDataQualityResponse response = tipDataQualityService.analyzeDataQuality(user, currency, severity, anomalyType);
        return ResponseEntity.ok(response);
    }
}
