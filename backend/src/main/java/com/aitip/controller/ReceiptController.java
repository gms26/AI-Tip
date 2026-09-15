package com.aitip.controller;

import com.aitip.dto.ReceiptAnalysisResponse;
import com.aitip.service.ReceiptOcrService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptOcrService receiptOcrService;

    public ReceiptController(ReceiptOcrService receiptOcrService) {
        this.receiptOcrService = receiptOcrService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptAnalysisResponse> analyzeReceipt(@RequestParam("file") MultipartFile file) {
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new com.aitip.exception.ReceiptOcrException("File size exceeds the 5MB limit");
        }
        ReceiptAnalysisResponse response = receiptOcrService.analyzeReceipt(file);
        return ResponseEntity.ok(response);
    }
}
