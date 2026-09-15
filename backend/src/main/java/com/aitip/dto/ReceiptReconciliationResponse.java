package com.aitip.dto;

import java.util.List;

public record ReceiptReconciliationResponse(
        ReceiptReconciliationStatus status,
        int matchCount,
        List<ReceiptMatchResponse> matches,
        String message
) {}
