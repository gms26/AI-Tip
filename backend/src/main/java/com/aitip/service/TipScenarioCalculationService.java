package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.util.CurrencyValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless, deterministic what-if tip scenario engine.
 *
 * <p><b>Core principle:</b> This service is the sole mathematical authority
 * for all scenario values. Gemini must never calculate, override, or invent
 * any of these values.</p>
 *
 * <p><b>This is a hypothetical scaling model, not a direct forecast.</b>
 * Monthly projections answer: "If I had historically tipped at this scenario
 * percentage instead of my actual median, what would my monthly spending
 * look like?" — clearly hypothetical, not predictive.</p>
 *
 * <p><b>Currency isolation:</b> Only tips matching the requested currency
 * contribute to that currency's historical baseline. Currencies are never mixed.</p>
 *
 * <p><b>Read-only:</b> This service never creates tips, modifies budgets,
 * triggers achievements, or persists any data.</p>
 */
@Service
public class TipScenarioCalculationService {

    private static final Logger log = LoggerFactory.getLogger(TipScenarioCalculationService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal THIRTY = new BigDecimal("30");
    private static final int DEFAULT_LOOKBACK_DAYS = 90;
    private static final BigDecimal DEFAULT_WARNING_THRESHOLD = new BigDecimal("80");

    private final TipRepository tipRepository;
    private final UserService userService;

    public TipScenarioCalculationService(TipRepository tipRepository, UserService userService) {
        this.tipRepository = tipRepository;
        this.userService = userService;
    }

    /**
     * Calculates what-if tip scenarios for the authenticated user.
     *
     * @param email   the authenticated user's email (from JWT)
     * @param request the validated scenario request
     * @return deterministic scenario response
     */
    @Transactional(readOnly = true)
    public TipScenarioResponse calculate(String email, TipScenarioRequest request) {
        User user = userService.getUserByEmail(email);
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(request.currency());

        // Validate monthly budget if supplied
        if (request.monthlyBudget() != null && request.monthlyBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly budget must be greater than 0.");
        }

        // =============================================
        // Historical data (90-day lookback, currency-isolated)
        // =============================================
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusDays(DEFAULT_LOOKBACK_DAYS);

        List<Tip> historicalTips = tipRepository
                .findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        user.getId(), normalizedCurrency, windowStart, now);

        int historicalTipCount = historicalTips.size();
        boolean hasHistory = historicalTipCount > 0;

        // =============================================
        // Historical statistics (null when no history)
        // =============================================
        BigDecimal historicalMedian = null;
        BigDecimal historicalMean = null;
        BigDecimal historicalAvgTipAmount = null;
        BigDecimal historicalMonthlyTipAmount = null;
        // Daily rate kept at high precision for intermediate calculations
        BigDecimal dailyRate = null;

        if (hasHistory) {
            List<BigDecimal> sortedPercentages = historicalTips.stream()
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .toList();

            List<BigDecimal> tipAmounts = historicalTips.stream()
                    .map(Tip::getTipAmount)
                    .toList();

            historicalMedian = calculateMedian(sortedPercentages);
            historicalMean = calculateMean(sortedPercentages);
            historicalAvgTipAmount = calculateMean(tipAmounts);

            // Daily rate at high precision (scale 6) — rounding applied only at final output
            dailyRate = new BigDecimal(historicalTipCount)
                    .divide(new BigDecimal(DEFAULT_LOOKBACK_DAYS), 6, RoundingMode.HALF_UP);

            // Historical monthly spending estimate (daily rate × 30 × avg tip amount)
            // Only round at the final monetary output
            if (historicalAvgTipAmount.compareTo(BigDecimal.ZERO) > 0) {
                historicalMonthlyTipAmount = dailyRate
                        .multiply(THIRTY)
                        .multiply(historicalAvgTipAmount)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        // =============================================
        // Scenario calculations (preserving input order)
        // =============================================
        BigDecimal billAmount = request.billAmount();

        // Historical tip amount for this specific bill (used for monetary difference)
        // Kept at high precision for intermediate use
        BigDecimal historicalTipForBill = null;
        if (hasHistory && historicalMedian != null) {
            historicalTipForBill = billAmount.multiply(historicalMedian)
                    .divide(HUNDRED, 6, RoundingMode.HALF_UP);
        }

        List<TipScenarioResult> scenarioResults = new ArrayList<>();

        for (BigDecimal scenarioPercentage : request.scenarioPercentages()) {
            // Direct arithmetic — always available
            BigDecimal tipAmount = billAmount.multiply(scenarioPercentage)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = billAmount.add(tipAmount);

            // Historical comparison — null when no history
            BigDecimal differenceFromHistorical = null;
            BigDecimal monetaryDifference = null;

            if (hasHistory && historicalMedian != null && historicalTipForBill != null) {
                differenceFromHistorical = scenarioPercentage.subtract(historicalMedian)
                        .setScale(2, RoundingMode.HALF_UP);
                monetaryDifference = tipAmount.subtract(
                        historicalTipForBill.setScale(2, RoundingMode.HALF_UP));
            }

            // Monthly what-if projection — requires history + budget + non-zero avg tip
            BigDecimal monthlyProjectedTipAmount = null;
            BigDecimal monthlyBudgetUsage = null;
            TipBudgetStatus budgetStatus = null;

            if (hasHistory && request.monthlyBudget() != null && dailyRate != null
                    && historicalAvgTipAmount != null
                    && historicalAvgTipAmount.compareTo(BigDecimal.ZERO) > 0) {

                // Hypothetical scaling model:
                // monthlyProjectedTipAmount = dailyRate × 30 × (scenarioTipAmount / historicalAvgTipAmount)
                //
                // This answers: "If I maintained my historical tipping frequency but
                // tipped at this scenario percentage, what would my monthly spending be?"
                //
                // Intermediate calculation uses high-precision (scale 6) to avoid
                // compounding rounding errors. Final monetary output rounds to scale 2.
                BigDecimal scalingRatio = tipAmount
                        .divide(historicalAvgTipAmount, 6, RoundingMode.HALF_UP);

                monthlyProjectedTipAmount = dailyRate
                        .multiply(THIRTY)
                        .multiply(scalingRatio)
                        // At this point we have: dailyRate(6dp) × 30 × ratio(6dp) = tip count
                        // Multiply by avg tip to get monetary: but we already have the ratio
                        // applied to tip amounts, so multiply back by historicalAvgTipAmount
                        .multiply(historicalAvgTipAmount)
                        .setScale(2, RoundingMode.HALF_UP);

                monthlyBudgetUsage = monthlyProjectedTipAmount
                        .multiply(HUNDRED)
                        .divide(request.monthlyBudget(), 2, RoundingMode.HALF_UP);

                budgetStatus = determineBudgetStatus(monthlyBudgetUsage);
            }

            scenarioResults.add(new TipScenarioResult(
                    scenarioPercentage,
                    tipAmount,
                    totalAmount,
                    differenceFromHistorical,
                    monetaryDifference,
                    monthlyProjectedTipAmount,
                    monthlyBudgetUsage,
                    budgetStatus
            ));
        }

        // =============================================
        // Message
        // =============================================
        String message = buildMessage(normalizedCurrency, historicalTipCount, historicalMedian,
                billAmount, scenarioResults.size(), request.monthlyBudget());

        log.debug("Scenario calculation for user {} / {}: historicalTips={}, " +
                        "historicalMedian={}, scenarios={}, hasBudget={}",
                user.getId(), normalizedCurrency, historicalTipCount,
                historicalMedian, scenarioResults.size(), request.monthlyBudget() != null);

        return new TipScenarioResponse(
                normalizedCurrency,
                billAmount,
                historicalMedian,
                historicalMean,
                historicalAvgTipAmount != null ? historicalAvgTipAmount.setScale(2, RoundingMode.HALF_UP) : null,
                historicalTipCount,
                scenarioResults,
                request.monthlyBudget(),
                historicalMonthlyTipAmount,
                message,
                null  // aiExplanation populated by controller if Gemini is available
        );
    }

    // =============================================
    // Statistical calculations (same proven patterns
    // as Day 24 TipForecastCalculationService)
    // =============================================

    /**
     * Calculates the median of a SORTED list of BigDecimals.
     * For odd count: middle value. For even count: (left + right) / 2.
     */
    BigDecimal calculateMedian(List<BigDecimal> sortedValues) {
        if (sortedValues.isEmpty()) return BigDecimal.ZERO;
        int size = sortedValues.size();
        if (size % 2 == 1) {
            return sortedValues.get(size / 2).setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal left = sortedValues.get(size / 2 - 1);
            BigDecimal right = sortedValues.get(size / 2);
            return left.add(right)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calculates the arithmetic mean of a list of BigDecimals.
     * Rounded to 2 decimal places using HALF_UP.
     */
    BigDecimal calculateMean(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Determines budget status using the same thresholds as Day 17.
     * Reuses {@link TipBudgetStatus} — no duplicate system.
     */
    TipBudgetStatus determineBudgetStatus(BigDecimal usagePercentage) {
        if (usagePercentage.compareTo(HUNDRED) > 0) {
            return TipBudgetStatus.OVER_BUDGET;
        }
        if (usagePercentage.compareTo(HUNDRED) == 0) {
            return TipBudgetStatus.LIMIT_REACHED;
        }
        if (usagePercentage.compareTo(DEFAULT_WARNING_THRESHOLD) >= 0) {
            return TipBudgetStatus.APPROACHING_LIMIT;
        }
        return TipBudgetStatus.UNDER_BUDGET;
    }

    private String buildMessage(String currency, int historicalCount,
                                BigDecimal historicalMedian, BigDecimal billAmount,
                                int scenarioCount, BigDecimal monthlyBudget) {
        StringBuilder sb = new StringBuilder();

        if (historicalCount == 0) {
            sb.append(String.format("No %s tipping history found. ", currency));
            sb.append("Scenario calculations are based on your input bill amount. ");
            sb.append("Historical comparison is unavailable.");
        } else {
            sb.append(String.format("Based on %d %s tip%s (last 90 days), your historical median is %s%%. ",
                    historicalCount, currency, historicalCount == 1 ? "" : "s",
                    historicalMedian.toPlainString()));
            sb.append(String.format("Comparing %d hypothetical scenario%s against a %s %s bill.",
                    scenarioCount, scenarioCount == 1 ? "" : "s",
                    currency, billAmount.toPlainString()));
        }

        if (monthlyBudget != null) {
            if (historicalCount == 0) {
                sb.append(" Monthly budget projection is unavailable without historical data.");
            } else {
                sb.append(String.format(" Monthly budget of %s %s applied for hypothetical projection.",
                        currency, monthlyBudget.toPlainString()));
            }
        }

        return sb.toString();
    }
}
