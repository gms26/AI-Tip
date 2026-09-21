package com.aitip.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for what-if tip scenario planning.
 *
 * <p><b>Core principle:</b> All values are deterministically calculated by
 * {@code TipScenarioCalculationService}. The optional {@code aiExplanation}
 * is produced by Groq explaining these facts; it may never override them.</p>
 *
 * <p><b>Important distinction:</b></p>
 * <ul>
 *   <li>Fields prefixed with {@code historical} are facts from the user's
 *       actual tip records. They are null when no history exists for the
 *       requested currency.</li>
 *   <li>{@code scenarioResults} are hypothetical projections clearly labeled
 *       as "what-if" estimates. They must never be presented as actual spending.</li>
 * </ul>
 *
 * @param currency                       ISO 4217 currency code
 * @param billAmount                     the bill used for scenario calculations
 * @param historicalMedianTipPercentage  median of user's actual tip % (nullable)
 * @param historicalAverageTipPercentage mean of user's actual tip % (nullable)
 * @param historicalAverageTipAmount     mean of user's actual tip amounts (nullable)
 * @param historicalTipCount             number of historical tips found
 * @param scenarioResults                one result per requested scenario percentage
 * @param monthlyBudget                  user-supplied budget (nullable)
 * @param historicalMonthlyTipAmount     estimated monthly spending from history (nullable)
 * @param message                        human-readable summary
 * @param aiExplanation                  optional Groq-generated explanation (nullable)
 */
public record TipScenarioResponse(
        String currency,
        BigDecimal billAmount,
        BigDecimal historicalMedianTipPercentage,
        BigDecimal historicalAverageTipPercentage,
        BigDecimal historicalAverageTipAmount,
        int historicalTipCount,
        List<TipScenarioResult> scenarioResults,
        BigDecimal monthlyBudget,
        BigDecimal historicalMonthlyTipAmount,
        String message,
        String aiExplanation
) {}
