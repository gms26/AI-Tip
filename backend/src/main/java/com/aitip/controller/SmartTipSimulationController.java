package com.aitip.controller;

import com.aitip.dto.SmartTipSimulationRequest;
import com.aitip.dto.SmartTipSimulationResponse;
import com.aitip.service.SmartTipSimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/smart-tip")
public class SmartTipSimulationController {

    private final SmartTipSimulationService simulationService;

    public SmartTipSimulationController(SmartTipSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<SmartTipSimulationResponse> simulate(
            Authentication authentication,
            @Valid @RequestBody SmartTipSimulationRequest request) {
        
        String email = authentication.getName();
        SmartTipSimulationResponse response = simulationService.simulate(email, request);
        return ResponseEntity.ok(response);
    }
}
