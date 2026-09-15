package com.aitip.dto;

import java.math.BigDecimal;

/**
 * One hypothetical scenario result within a what-if analysis.
 *
 * <p><b>All values are deterministically calculated by
 * {@code TipScenarioCalculationService}.</b> Gemini must never
 * recalculate, override, or invent any of these values.</p>
 *
 * <p><b>Important distinction:</b></p>
 * <ul>
 *   <li>{@code tipPercentage}, {@code tipAmount}, {@code totalAmount} — direct
 *       arithmetic from user input (always available).</li>
 *   <li>{@code differenceFromHistorical}, {@code monetaryDifference} — require
 *       historical data; null when no history exists.</li>
 *   <li>{@code monthlyProjectedTipAmount}, {@code monthlyBudgetUsagePercentage},
 *       {@code budgetStatus} — require both historical data AND a supplied
 *       monthly budget; null otherwise.</li>
 * </ul>
 *
 * @param tipPercentage                 the hypothetical tip percentage
 * @param tipAmount                     billAmount × tipPercentage / 100
 * @param totalAmount                   billAmount + tipAmount
 * @param differenceFromHistorical      percentage-point difference from historical median (nullable)
 * @param monetaryDifference            tip amount difference vs historical tip for same bill (nullable)
 * @param monthlyProjectedTipAmount     hypothetical monthly spending at this rate (nullable)
 * @param monthlyBudgetUsagePercentage  % of budget this scenario would use monthly (nullable)
 * @param budgetStatus                  Day 17 budget classification (nullable)
 */
public record TipScenarioResult(
        BigDecimal tipPercentage,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        BigDecimal differenceFromHistorical,
        BigDecimal monetaryDifference,
        BigDecimal monthlyProjectedTipAmount,
        BigDecimal monthlyBudgetUsagePercentage,
        TipBudgetStatus budgetStatus
) {}
