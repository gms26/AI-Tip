package com.aitip.controller;

import com.aitip.dto.PaymentSessionRequest;
import com.aitip.dto.PaymentSessionResponse;
import com.aitip.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/session")
    public ResponseEntity<PaymentSessionResponse> createSession(
            @Valid @RequestBody PaymentSessionRequest request,
            Authentication authentication) {
        
        PaymentSessionResponse response = paymentService.createSession(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentSessionResponse> getSession(
            @PathVariable UUID id,
            Authentication authentication) {
        
        PaymentSessionResponse response = paymentService.getSession(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentSessionResponse> cancelSession(
            @PathVariable UUID id,
            Authentication authentication) {
        
        PaymentSessionResponse response = paymentService.cancelSession(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
