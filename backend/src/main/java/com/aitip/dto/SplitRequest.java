package com.aitip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for split tip calculations.
 *
 * <p><b>Purpose:</b> Input for the stateless split calculation endpoint.</p>
 *
 * <p><b>Features:</b>
 * Supports both equal splits (just provide a list of names) and custom
 * splits (provide a map of names to their specific bill portions).</p>
 *
 * @param billAmount     Total bill amount before tip
 * @param tipPercentage  Tip percentage to apply
 * @param people         List of names for an EQUAL split
 * @param customSplit    Map of name -> amount for a CUSTOM split. 
 *                       If provided, 'people' is ignored.
 */
public record SplitRequest(
        @NotNull(message = "Bill amount is required")
        @DecimalMin(value = "0.01", message = "Bill amount must be greater than 0")
        BigDecimal billAmount,

        @NotNull(message = "Tip percentage is required")
        @DecimalMin(value = "0.00", message = "Tip percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Tip percentage cannot exceed 100")
        BigDecimal tipPercentage,

        @Size(min = 2, message = "Split requires at least 2 people")
        List<String> people,
        
        Map<String, BigDecimal> customSplit
) {
}
