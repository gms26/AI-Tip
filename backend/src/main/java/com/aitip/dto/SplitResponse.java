package com.aitip.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for split tip response.
 *
 * <p><b>Purpose:</b> Returns the calculation of a split tip.</p>
 */
public record SplitResponse(
        BigDecimal billAmount,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        BigDecimal tipPercentage,
        int numberOfPeople,
        List<PersonSplit> splits
) {
    /**
     * Nested record for individual person's split breakdown.
     */
    public record PersonSplit(
            String name,
            BigDecimal amountOwed
    ) {}
}
