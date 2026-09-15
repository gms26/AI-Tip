package com.aitip.controller;

import com.aitip.dto.TipHistorySearchRequest;
import com.aitip.dto.TipHistorySearchResponse;
import com.aitip.service.TipHistorySearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tips/history")
@RequiredArgsConstructor
public class TipHistoryController {

    private final TipHistorySearchService searchService;

    @PostMapping("/search")
    public ResponseEntity<TipHistorySearchResponse> searchTipHistory(
            Authentication authentication,
            @Valid @RequestBody TipHistorySearchRequest request) {
        
        String email = authentication.getName();
        TipHistorySearchResponse response = searchService.searchHistory(email, request);
        return ResponseEntity.ok(response);
    }
}
