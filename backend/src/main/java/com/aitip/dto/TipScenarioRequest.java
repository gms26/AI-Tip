package com.aitip.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for what-if tip scenario planning.
 *
 * <p><b>Purpose:</b> Allows a user to compare multiple hypothetical tip
 * percentages against a given bill amount, optionally projecting monthly
 * budget impact.</p>
 *
 * <p><b>Security:</b> No userId field. The authenticated identity
 * comes exclusively from JWT ({@code Authentication.getName()}).</p>
 *
 * <p><b>Read-only operation:</b> Submitting a scenario never creates a Tip,
 * modifies budgets, or triggers achievement evaluation.</p>
 *
 * @param currency             ISO 4217 currency code (required)
 * @param billAmount           the bill to calculate scenarios against (required, > 0)
 * @param scenarioPercentages  1â€“5 hypothetical tip percentages, each 0â€“100 (required)
 * @param monthlyBudget        optional monthly budget for projection (must be > 0 if supplied)
 */
public record TipScenarioRequest(
        @NotBlank(message = "Currency is required")
        String currency,

        @NotNull(message = "Bill amount is required")
        @DecimalMin(value = "0.01", message = "Bill amount must be greater than 0")
        BigDecimal billAmount,

        @NotNull(message = "At least one scenario percentage is required")
        @Size(min = 1, max = 5, message = "Provide between 1 and 5 scenario percentages")
        List<@NotNull(message = "Scenario percentage cannot be null")
             @DecimalMin(value = "0", message = "Scenario percentage cannot be negative")
             @DecimalMax(value = "100", message = "Scenario percentage cannot exceed 100")
             BigDecimal> scenarioPercentages,

        BigDecimal monthlyBudget
) {}
