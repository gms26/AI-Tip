package com.aitip.controller;

import com.aitip.dto.insights.TipInsightSummaryResponse;
import com.aitip.service.TipInsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class TipInsightController {

    private final TipInsightService tipInsightService;

    public TipInsightController(TipInsightService tipInsightService) {
        this.tipInsightService = tipInsightService;
    }

    @GetMapping("/tips")
    public ResponseEntity<TipInsightSummaryResponse> getTipsInsights(Authentication authentication) {
        TipInsightSummaryResponse response = tipInsightService.getInsights(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
