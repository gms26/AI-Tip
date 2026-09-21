package com.aitip.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for tip responses.
 *
 * <p><b>Purpose:</b> Safe data transfer object returned to clients.
 * Excludes the user object (caller already knows who they are) to
 * avoid circular references and unnecessary payload size.</p>
 *
 * <p><b>Day 6 â€” serviceQuality:</b>
 * May be {@code null} for tips created before Day 6. Clients should
 * treat a null value as "not rated" and display nothing or a placeholder.</p>
 */
public record TipResponse(
        UUID id,
        String restaurantName,
        BigDecimal billAmount,
        BigDecimal tipPercentage,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime createdAt,
        ServiceQuality serviceQuality
) {
}
