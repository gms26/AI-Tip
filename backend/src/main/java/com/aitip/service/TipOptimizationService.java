package com.aitip.service;

import com.aitip.dto.BudgetConfidence;
import com.aitip.dto.TipOptimizationRequest;
import com.aitip.dto.TipOptimizationResponse;
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
import java.util.List;

/**
 * Deterministic tip optimization calculations.
 *
 * <p><b>Core principle:</b> This service is the sole mathematical authority
 * for all optimization statistics. Groq must never calculate, override,
 * or invent any of these values.</p>
 *
 * <p><b>Currency isolation:</b> Only tips matching the requested currency
 * contribute to that currency's optimization calculations. Currencies
 * are never combined or converted.</p>
 *
 * <p><b>Budget distinction:</b> The {@code monthlyBudget} field in the request
 * is a user-supplied assumption for estimation purposes. It is NOT the user's
 * actual Day 17 budget. Response fields clearly label these as estimates.</p>
 */
@Service
public class TipOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(TipOptimizationService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    private final TipRepository tipRepository;
    private final UserService userService;

    public TipOptimizationService(TipRepository tipRepository, UserService userService) {
        this.tipRepository = tipRepository;
        this.userService = userService;
    }

    /**
     * Calculates tip optimization for the authenticated user.
     *
     * @param email   the authenticated user's email (from JWT)
     * @param request the optimization parameters
     * @return deterministic optimization response
     */
    @Transactional(readOnly = true)
    public TipOptimizationResponse optimize(String email, TipOptimizationRequest request) {
        User user = userService.getUserByEmail(email);
        String currency = CurrencyValidationUtil.normalizeAndValidate(request.currency());

        // Fetch all tips for the user, then filter by currency
        List<Tip> allTips = tipRepository.findAllByUserId(user.getId());
        List<Tip> currencyTips = allTips.stream()
                .filter(tip -> currency.equals(tip.getCurrency()))
                .toList();

        int sampleSize = currencyTips.size();

        // No history for this currency â€” return empty state
        if (sampleSize == 0) {
            return buildEmptyResponse(currency, request.currentTipPercentage());
        }

        // Extract and sort tip percentages
        List<BigDecimal> sortedPercentages = currencyTips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .toList();

        BigDecimal median = calculateMedian(sortedPercentages);
        BigDecimal mean = calculateMean(sortedPercentages);

        // Target range: median Â± 2, clamped to [0, 100]
        BigDecimal recommendedMin = median.subtract(TWO).max(BigDecimal.ZERO);
        BigDecimal recommendedMax = median.add(TWO).min(HUNDRED);

        // Confidence
        BudgetConfidence confidence = determineConfidence(sampleSize);

        // Monthly estimation (only when budget is supplied)
        BigDecimal monthlyTipEstimate = null;
        BigDecimal optimizedMonthlyTipEstimate = null;
        BigDecimal potentialMonthlyDifference = null;

        if (request.monthlyBudget() != null) {
            monthlyTipEstimate = request.monthlyBudget()
                    .multiply(request.currentTipPercentage())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            // Only calculate optimized/difference when we have history (median exists)
            optimizedMonthlyTipEstimate = request.monthlyBudget()
                    .multiply(median)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            potentialMonthlyDifference = monthlyTipEstimate.subtract(optimizedMonthlyTipEstimate);
        }

        // Bill-level estimation (only when bill amount is supplied)
        BigDecimal currentBillTip = null;
        BigDecimal medianBillTip = null;
        BigDecimal billDifference = null;

        if (request.billAmount() != null) {
            currentBillTip = request.billAmount()
                    .multiply(request.currentTipPercentage())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            medianBillTip = request.billAmount()
                    .multiply(median)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            billDifference = currentBillTip.subtract(medianBillTip);
        }

        // Build message
        String message = buildMessage(currency, median, mean, request.currentTipPercentage(),
                sampleSize, confidence, billDifference, potentialMonthlyDifference);

        log.debug("Optimization for user {} / {}: median={}, mean={}, sampleSize={}, confidence={}",
                user.getId(), currency, median, mean, sampleSize, confidence);

        return new TipOptimizationResponse(
                currency,
                request.currentTipPercentage(),
                median,
                mean,
                recommendedMin,
                recommendedMax,
                sampleSize,
                confidence,
                monthlyTipEstimate,
                optimizedMonthlyTipEstimate,
                potentialMonthlyDifference,
                message
        );
    }

    // =============================================
    // Statistical calculations
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
     * Determines confidence based on sample size.
     * Uses the existing project convention from BudgetConfidence:
     * 0â€“1 â†’ LOW, 2â€“4 â†’ MEDIUM, 5+ â†’ HIGH
     */
    BudgetConfidence determineConfidence(int sampleSize) {
        if (sampleSize >= 5) return BudgetConfidence.HIGH;
        if (sampleSize >= 2) return BudgetConfidence.MEDIUM;
        return BudgetConfidence.LOW;
    }

    // =============================================
    // Private helpers
    // =============================================

    private TipOptimizationResponse buildEmptyResponse(String currency, BigDecimal currentTipPercentage) {
        return new TipOptimizationResponse(
                currency,
                currentTipPercentage,
                null,   // historicalMedianPercentage
                null,   // historicalMeanPercentage
                null,   // recommendedMinimumPercentage
                null,   // recommendedMaximumPercentage
                0,      // sampleSize
                BudgetConfidence.LOW,
                null,   // monthlyTipEstimate
                null,   // optimizedMonthlyTipEstimate
                null,   // potentialMonthlyDifference
                "No tipping history for " + currency + ". Save some tips to see personalized optimization insights."
        );
    }

    private String buildMessage(String currency, BigDecimal median, BigDecimal mean,
                                BigDecimal currentTipPercentage, int sampleSize,
                                BudgetConfidence confidence, BigDecimal billDifference,
                                BigDecimal potentialMonthlyDifference) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Based on %d %s tip%s, your historical median is %s%% and mean is %s%%.",
                sampleSize, currency, sampleSize == 1 ? "" : "s",
                median.toPlainString(), mean.toPlainString()));

        int comparison = currentTipPercentage.compareTo(median);
        if (comparison > 0) {
            sb.append(String.format(" Your current %s%% is above your historical median.",
                    currentTipPercentage.toPlainString()));
        } else if (comparison < 0) {
            sb.append(String.format(" Your current %s%% is below your historical median.",
                    currentTipPercentage.toPlainString()));
        } else {
            sb.append(" Your current tip matches your historical median.");
        }

        if (confidence == BudgetConfidence.LOW) {
            sb.append(" Note: limited history â€” these statistics may change as you record more tips.");
        }

        return sb.toString();
    }
}
