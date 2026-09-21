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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Stateless, deterministic tip forecasting engine.
 *
 * <p><b>Core principle:</b> This service is the sole mathematical authority
 * for all forecast values. Groq must never calculate, override, or invent
 * any of these values.</p>
 *
 * <p><b>Currency isolation:</b> Only tips matching the requested currency
 * contribute to that currency's forecast. Currencies are never mixed.</p>
 *
 * <p><b>Forecast method:</b> Transparent deterministic baseline using
 * historical daily tip rate extrapolation. No machine learning.</p>
 *
 * <p><b>Read-only:</b> This service never modifies tips, budgets, or any
 * other persisted data.</p>
 */
@Service
public class TipForecastCalculationService {

    private static final Logger log = LoggerFactory.getLogger(TipForecastCalculationService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal THIRTY = new BigDecimal("30");
    private static final int DEFAULT_LOOKBACK_DAYS = 90;
    private static final int MIN_LOOKBACK_DAYS = 7;
    private static final int MAX_LOOKBACK_DAYS = 365;
    // Default warning threshold matching Day 17 budget rules
    private static final BigDecimal DEFAULT_WARNING_THRESHOLD = new BigDecimal("80");

    private final TipRepository tipRepository;
    private final UserService userService;

    public TipForecastCalculationService(TipRepository tipRepository, UserService userService) {
        this.tipRepository = tipRepository;
        this.userService = userService;
    }

    /**
     * Calculates a tip forecast for the authenticated user.
     *
     * @param email          the authenticated user's email (from JWT)
     * @param currency       ISO 4217 currency code (required)
     * @param period         forecast period (required)
     * @param monthlyBudget  optional user-supplied monthly budget
     * @param lookbackDays   optional lookback window (default 90, range 7â€“365)
     * @return deterministic forecast response
     */
    @Transactional(readOnly = true)
    public TipForecastResponse forecast(String email, String currency, TipForecastPeriod period,
                                        BigDecimal monthlyBudget, Integer lookbackDays) {
        User user = userService.getUserByEmail(email);
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);

        // Validate and default lookback
        int effectiveLookback = validateLookbackDays(lookbackDays);

        // Validate monthly budget if supplied
        if (monthlyBudget != null && monthlyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly budget must be greater than 0.");
        }

        // Historical window: [now - lookbackDays, now)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusDays(effectiveLookback);

        // Query tips filtered by user, currency, and date range at DB level
        List<Tip> historicalTips = tipRepository
                .findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        user.getId(), normalizedCurrency, windowStart, now);

        int historicalTipCount = historicalTips.size();

        // Determine forecast days based on period
        int forecastDays = calculateForecastDays(period);

        // Zero-history: return empty state
        if (historicalTipCount == 0) {
            return buildEmptyResponse(normalizedCurrency, period, forecastDays, effectiveLookback, monthlyBudget);
        }

        // =============================================
        // Historical statistics (BigDecimal, all facts)
        // =============================================

        List<BigDecimal> sortedPercentages = historicalTips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .toList();

        List<BigDecimal> tipAmounts = historicalTips.stream()
                .map(Tip::getTipAmount)
                .toList();

        BigDecimal historicalAveragePercentage = calculateMean(sortedPercentages);
        BigDecimal historicalMedianPercentage = calculateMedian(sortedPercentages);
        BigDecimal historicalAverageTipAmount = calculateMean(tipAmounts);

        // =============================================
        // Forecast projections (deterministic baseline)
        // =============================================

        // Daily tip rate: historicalTipCount / lookbackDays (scale 4 for precision)
        BigDecimal dailyRate = new BigDecimal(historicalTipCount)
                .divide(new BigDecimal(effectiveLookback), 4, RoundingMode.HALF_UP);

        // Estimated tip count: dailyRate Ã— forecastDays, rounded to nearest int
        BigDecimal rawEstimatedCount = dailyRate.multiply(new BigDecimal(forecastDays));
        int estimatedTipCount = rawEstimatedCount.setScale(0, RoundingMode.HALF_UP).intValue();

        // Estimated spending: estimatedTipCount Ã— historicalAverageTipAmount
        BigDecimal estimatedMonthlyTipAmount = new BigDecimal(estimatedTipCount)
                .multiply(historicalAverageTipAmount)
                .setScale(2, RoundingMode.HALF_UP);

        // Projected tip percentage: use median (more resistant to outliers)
        BigDecimal projectedTipPercentage = historicalMedianPercentage;

        // Confidence
        BudgetConfidence confidence = determineConfidence(historicalTipCount);

        // =============================================
        // Budget projection (optional)
        // =============================================
        BigDecimal projectedBudgetUsage = null;
        TipBudgetStatus budgetStatus = null;

        if (monthlyBudget != null) {
            // For multi-month periods, normalize to monthly rate
            BigDecimal monthlyProjectedAmount;
            if (forecastDays > 31) {
                // Normalize: (estimatedAmount / forecastDays) Ã— 30
                monthlyProjectedAmount = estimatedMonthlyTipAmount
                        .divide(new BigDecimal(forecastDays), 4, RoundingMode.HALF_UP)
                        .multiply(THIRTY)
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                monthlyProjectedAmount = estimatedMonthlyTipAmount;
            }

            projectedBudgetUsage = monthlyProjectedAmount
                    .multiply(HUNDRED)
                    .divide(monthlyBudget, 2, RoundingMode.HALF_UP);

            budgetStatus = determineBudgetStatus(projectedBudgetUsage);
        }

        // Message
        String message = buildMessage(normalizedCurrency, historicalTipCount, effectiveLookback,
                historicalAverageTipAmount, historicalMedianPercentage,
                estimatedTipCount, estimatedMonthlyTipAmount, confidence, period, budgetStatus);

        log.debug("Forecast for user {} / {}: lookback={}d, historical={} tips, " +
                        "dailyRate={}, forecastDays={}, estimatedCount={}, estimatedAmount={}, confidence={}",
                user.getId(), normalizedCurrency, effectiveLookback, historicalTipCount,
                dailyRate, forecastDays, estimatedTipCount, estimatedMonthlyTipAmount, confidence);

        return new TipForecastResponse(
                normalizedCurrency,
                period,
                forecastDays,
                effectiveLookback,
                historicalTipCount,
                historicalAveragePercentage,
                historicalMedianPercentage,
                historicalAverageTipAmount,
                estimatedMonthlyTipAmount,
                estimatedTipCount,
                projectedTipPercentage,
                confidence,
                monthlyBudget,
                projectedBudgetUsage,
                budgetStatus,
                message
        );
    }

    // =============================================
    // Statistical calculations (same proven patterns
    // as TipOptimizationService)
    // =============================================

    /**
     * Calculates the median of a SORTED list of BigDecimals.
     *
     * <p>For odd count: middle value.
     * For even count: (left + right) / 2 with HALF_UP rounding to 2dp.</p>
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
     * Determines confidence based on historical tip count.
     * Uses the existing project convention from BudgetConfidence:
     * 0â€“1 â†’ LOW, 2â€“4 â†’ MEDIUM, 5+ â†’ HIGH
     */
    BudgetConfidence determineConfidence(int tipCount) {
        if (tipCount >= 5) return BudgetConfidence.HIGH;
        if (tipCount >= 2) return BudgetConfidence.MEDIUM;
        return BudgetConfidence.LOW;
    }

    /**
     * Determines budget status using the same thresholds as Day 17.
     * Uses 80% as the default warning threshold.
     */
    TipBudgetStatus determineBudgetStatus(BigDecimal projectedUsage) {
        if (projectedUsage.compareTo(HUNDRED) > 0) {
            return TipBudgetStatus.OVER_BUDGET;
        }
        if (projectedUsage.compareTo(HUNDRED) == 0) {
            return TipBudgetStatus.LIMIT_REACHED;
        }
        if (projectedUsage.compareTo(DEFAULT_WARNING_THRESHOLD) >= 0) {
            return TipBudgetStatus.APPROACHING_LIMIT;
        }
        return TipBudgetStatus.UNDER_BUDGET;
    }

    // =============================================
    // Private helpers
    // =============================================

    /**
     * Calculates the number of forecast days based on the period.
     */
    int calculateForecastDays(TipForecastPeriod period) {
        return switch (period) {
            case CURRENT_MONTH -> {
                LocalDate today = LocalDate.now();
                int daysInMonth = today.lengthOfMonth();
                int dayOfMonth = today.getDayOfMonth();
                yield daysInMonth - dayOfMonth;
            }
            case NEXT_30_DAYS -> 30;
            case NEXT_3_MONTHS -> 90;
        };
    }

    private int validateLookbackDays(Integer lookbackDays) {
        if (lookbackDays == null) return DEFAULT_LOOKBACK_DAYS;
        if (lookbackDays < MIN_LOOKBACK_DAYS || lookbackDays > MAX_LOOKBACK_DAYS) {
            throw new IllegalArgumentException(
                    String.format("Lookback days must be between %d and %d.", MIN_LOOKBACK_DAYS, MAX_LOOKBACK_DAYS));
        }
        return lookbackDays;
    }

    private TipForecastResponse buildEmptyResponse(String currency, TipForecastPeriod period,
                                                    int forecastDays, int lookbackDays,
                                                    BigDecimal monthlyBudget) {
        return new TipForecastResponse(
                currency,
                period,
                forecastDays,
                lookbackDays,
                0,                  // historicalTipCount
                BigDecimal.ZERO,    // historicalAveragePercentage
                BigDecimal.ZERO,    // historicalMedianPercentage
                BigDecimal.ZERO,    // historicalAverageTipAmount
                BigDecimal.ZERO,    // estimatedMonthlyTipAmount
                0,                  // estimatedTipCount
                BigDecimal.ZERO,    // projectedTipPercentage
                BudgetConfidence.LOW,
                monthlyBudget,
                null,               // projectedBudgetUsage
                null,               // budgetStatus
                "No tipping history for " + currency + " in the last " + lookbackDays +
                        " days. Start tipping to see spending forecasts."
        );
    }

    private String buildMessage(String currency, int historicalCount, int lookbackDays,
                                BigDecimal avgTipAmount, BigDecimal medianPercentage,
                                int estimatedCount, BigDecimal estimatedAmount,
                                BudgetConfidence confidence, TipForecastPeriod period,
                                TipBudgetStatus budgetStatus) {
        StringBuilder sb = new StringBuilder();

        // Historical fact
        sb.append(String.format("Based on %d %s tip%s in the last %d days, your average tip was %s %s " +
                        "with a median percentage of %s%%.",
                historicalCount, currency, historicalCount == 1 ? "" : "s",
                lookbackDays, currency, avgTipAmount.toPlainString(),
                medianPercentage.toPlainString()));

        // Projection (clearly labeled as estimate)
        String periodLabel = switch (period) {
            case CURRENT_MONTH -> "for the remainder of this month";
            case NEXT_30_DAYS -> "over the next 30 days";
            case NEXT_3_MONTHS -> "over the next 3 months";
        };
        sb.append(String.format(" Estimated from your recent history: ~%d tip%s %s, " +
                        "totaling approximately %s %s.",
                estimatedCount, estimatedCount == 1 ? "" : "s", periodLabel,
                currency, estimatedAmount.toPlainString()));

        if (confidence == BudgetConfidence.LOW) {
            sb.append(" Note: limited history â€” this forecast may change as you record more tips.");
        }

        if (budgetStatus != null && budgetStatus == TipBudgetStatus.OVER_BUDGET) {
            sb.append(" âš ï¸ Your projected spending exceeds your monthly budget.");
        } else if (budgetStatus != null && budgetStatus == TipBudgetStatus.APPROACHING_LIMIT) {
            sb.append(" Your projected spending is approaching your monthly budget limit.");
        }

        return sb.toString();
    }
}
