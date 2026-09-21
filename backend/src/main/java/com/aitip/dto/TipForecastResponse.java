package com.aitip.dto;

import java.math.BigDecimal;

/**
 * Response DTO for tip forecasting.
 *
 * <p><b>Core principle:</b> All values are deterministically calculated by
 * {@code TipForecastCalculationService}. Groq may only explain these facts.</p>
 *
 * <p><b>Important distinction:</b>
 * <ul>
 *   <li>Fields prefixed with {@code historical} are facts from the lookback window.</li>
 *   <li>Fields prefixed with {@code estimated} or {@code projected} are projections
 *       derived from those facts. They must never be presented as guaranteed.</li>
 * </ul></p>
 *
 * @param currency                    ISO 4217 currency code
 * @param forecastPeriod              the forecast period requested
 * @param forecastDays                number of days in the forecast window
 * @param lookbackDays                number of days in the historical window
 * @param historicalTipCount          tips found in the lookback window
 * @param historicalAveragePercentage mean tip percentage (scale 2, HALF_UP)
 * @param historicalMedianPercentage  median tip percentage (scale 2, HALF_UP)
 * @param historicalAverageTipAmount  mean tip amount (scale 2, HALF_UP)
 * @param estimatedMonthlyTipAmount   projected spending for the forecast period
 * @param estimatedTipCount           projected tip count for the forecast period
 * @param projectedTipPercentage      projected tip % (uses median)
 * @param confidence                  LOW / MEDIUM / HIGH
 * @param monthlyBudget               user-supplied budget (nullable)
 * @param projectedBudgetUsage        % of budget projected to be used (nullable)
 * @param budgetStatus                budget status classification (nullable)
 * @param message                     human-readable summary
 */
public record TipForecastResponse(
        String currency,
        TipForecastPeriod forecastPeriod,
        int forecastDays,
        int lookbackDays,
        int historicalTipCount,
        BigDecimal historicalAveragePercentage,
        BigDecimal historicalMedianPercentage,
        BigDecimal historicalAverageTipAmount,
        BigDecimal estimatedMonthlyTipAmount,
        int estimatedTipCount,
        BigDecimal projectedTipPercentage,
        BudgetConfidence confidence,
        BigDecimal monthlyBudget,
        BigDecimal projectedBudgetUsage,
        TipBudgetStatus budgetStatus,
        String message
) {}
