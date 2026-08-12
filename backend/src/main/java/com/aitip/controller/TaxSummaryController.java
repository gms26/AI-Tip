package com.aitip.controller;

import com.aitip.dto.TaxSummaryRequest;
import com.aitip.dto.TaxSummaryResponse;
import com.aitip.service.TaxSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxSummaryController {

    private final TaxSummaryService taxSummaryService;

    @PostMapping("/summary")
    public ResponseEntity<TaxSummaryResponse> getSummary(
            @RequestBody TaxSummaryRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        TaxSummaryResponse response = taxSummaryService.getSummary(email, request);
        return ResponseEntity.ok(response);
    }
}
