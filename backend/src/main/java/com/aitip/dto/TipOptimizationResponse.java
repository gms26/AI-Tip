package com.aitip.dto;

import java.math.BigDecimal;

/**
 * Response DTO for tip optimization analysis.
 *
 * <p><b>Design:</b> All monetary and statistical fields use BigDecimal.
 * Nullable fields indicate "not enough data" or "not applicable."</p>
 *
 * <p><b>Important distinction:</b></p>
 * <ul>
 *   <li>{@code monthlyTipEstimate} and {@code optimizedMonthlyTipEstimate} are
 *       <b>estimated</b> values based on the user-supplied {@code monthlyBudget}
 *       assumption. They are NOT actual historical spending figures.</li>
 *   <li>{@code potentialMonthlyDifference} is only populated when both a
 *       {@code monthlyBudget} is supplied AND sufficient historical data exists
 *       to calculate a median. Otherwise it is {@code null}.</li>
 * </ul>
 *
 * @param currency                      ISO 4217 currency code
 * @param currentTipPercentage          The user's current/input tip percentage
 * @param historicalMedianPercentage    Median tip percentage from history (null if no history)
 * @param historicalMeanPercentage      Mean tip percentage from history (null if no history)
 * @param recommendedMinimumPercentage  max(0, median - 2), null if no history
 * @param recommendedMaximumPercentage  min(100, median + 2), null if no history
 * @param sampleSize                    Number of tips in the currency-scoped history
 * @param confidence                    Deterministic confidence based on sample size
 * @param monthlyTipEstimate            Estimated monthly tip based on supplied budget (null if no budget)
 * @param optimizedMonthlyTipEstimate   Estimated monthly tip at median percentage (null if no budget or no history)
 * @param potentialMonthlyDifference    Difference between current and optimized estimates (null if not calculable)
 * @param message                       Human-readable summary using neutral language
 */
public record TipOptimizationResponse(
        String currency,
        BigDecimal currentTipPercentage,
        BigDecimal historicalMedianPercentage,
        BigDecimal historicalMeanPercentage,
        BigDecimal recommendedMinimumPercentage,
        BigDecimal recommendedMaximumPercentage,
        int sampleSize,
        BudgetConfidence confidence,
        BigDecimal monthlyTipEstimate,
        BigDecimal optimizedMonthlyTipEstimate,
        BigDecimal potentialMonthlyDifference,
        String message
) {}
