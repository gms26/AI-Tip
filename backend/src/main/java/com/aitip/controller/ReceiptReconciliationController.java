package com.aitip.controller;

import com.aitip.dto.ReceiptReconciliationRequest;
import com.aitip.dto.ReceiptReconciliationResponse;
import com.aitip.service.ReceiptReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptReconciliationController {

    private final ReceiptReconciliationService reconciliationService;

    @PostMapping("/reconcile")
    public ResponseEntity<ReceiptReconciliationResponse> reconcileReceipt(
            @Valid @RequestBody ReceiptReconciliationRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        ReceiptReconciliationResponse response = reconciliationService.reconcile(email, request);
        return ResponseEntity.ok(response);
    }
}
