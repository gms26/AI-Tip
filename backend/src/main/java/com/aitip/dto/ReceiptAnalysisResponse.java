package com.aitip.dto;

import java.math.BigDecimal;

public record ReceiptAnalysisResponse(
        BigDecimal billAmount,
        String restaurantName,
        String currency
) {
}
