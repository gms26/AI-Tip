package com.aitip.controller;

import com.aitip.dto.CurrencyConversionRequest;
import com.aitip.dto.CurrencyConversionResponse;
import com.aitip.service.CurrencyConversionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for currency conversion operations.
 * Requires JWT authentication via security config.
 */
@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    private final CurrencyConversionService currencyConversionService;

    public CurrencyController(CurrencyConversionService currencyConversionService) {
        this.currencyConversionService = currencyConversionService;
    }

    /**
     * Converts a specific amount between currencies.
     */
    @PostMapping("/convert")
    public ResponseEntity<CurrencyConversionResponse> convert(@Valid @RequestBody CurrencyConversionRequest request) {
        CurrencyConversionResponse response = currencyConversionService.convert(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetches only the exchange rate for local calculations.
     */
    @GetMapping("/rate")
    public ResponseEntity<CurrencyConversionResponse> getRate(
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        
        CurrencyConversionResponse response = currencyConversionService.getExchangeRateOnly(from, to);
        return ResponseEntity.ok(response);
    }
}
