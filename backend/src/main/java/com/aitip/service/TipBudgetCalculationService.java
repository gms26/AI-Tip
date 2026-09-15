package com.aitip.service;

import com.aitip.dto.BudgetConfidence;
import com.aitip.dto.TipBudgetStatus;
import com.aitip.dto.TipBudgetStatusResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.TipBudget;
import com.aitip.repository.TipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Pure deterministic budget calculations using BigDecimal.
 *
 * <p><b>Core principle:</b> This service is the sole authority for
 * budget status. Gemini must never calculate, override, or invent
 * any of these values.</p>
 *
 * <p><b>Currency isolation:</b> Only tips matching the budget's
 * currency contribute to that budget's spending total.</p>
 */
@Service
public class TipBudgetCalculationService {

    private static final Logger log = LoggerFactory.getLogger(TipBudgetCalculationService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final TipRepository tipRepository;

    public TipBudgetCalculationService(TipRepository tipRepository) {
        this.tipRepository = tipRepository;
    }

    /**
     * Calculates the full budget status for a given budget.
     *
     * @param budget  the user's budget configuration
     * @param userId  the authenticated user's ID
     * @return deterministic status response
     */
    public TipBudgetStatusResponse calculateStatus(TipBudget budget, UUID userId) {
        // Current calendar month boundaries: [first day of month, first day of next month)
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        // Fetch tips filtered by user, currency, and date range at DB level
        List<Tip> currentMonthTips = tipRepository
                .findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        userId, budget.getCurrency(), monthStart, monthEnd);

        int tipCount = currentMonthTips.size();

        // Sum tip amounts using BigDecimal
        BigDecimal totalSpent = currentMonthTips.stream()
                .map(Tip::getTipAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyLimit = budget.getMonthlyLimit();
        BigDecimal warningThreshold = budget.getWarningThreshold();

        // Remaining budget (can be negative when over budget)
        BigDecimal remainingBudget = monthlyLimit.subtract(totalSpent);

        // Percentage used: (totalSpent / monthlyLimit) * 100, rounded HALF_UP to 2dp
        BigDecimal percentageUsed = totalSpent
                .multiply(HUNDRED)
                .divide(monthlyLimit, 2, RoundingMode.HALF_UP);

        // Deterministic status (evaluation order matters)
        TipBudgetStatus status = determineStatus(tipCount, percentageUsed, warningThreshold);

        // Deterministic confidence
        BudgetConfidence confidence = determineConfidence(tipCount);

        // Human-readable message
        String message = generateMessage(status, percentageUsed, remainingBudget, budget.getCurrency());

        log.debug("Budget status for user {} / {}: {}% used, status={}, confidence={}",
                userId, budget.getCurrency(), percentageUsed, status, confidence);

        return new TipBudgetStatusResponse(
                budget.getCurrency(),
                monthlyLimit,
                totalSpent,
                remainingBudget,
                percentageUsed,
                warningThreshold,
                status,
                tipCount,
                confidence,
                message
        );
    }

    /**
     * Determines the budget status using the locked evaluation order:
     * <ol>
     *   <li>NO_HISTORY if tipCount == 0</li>
     *   <li>OVER_BUDGET if percentageUsed > 100</li>
     *   <li>LIMIT_REACHED if percentageUsed == 100</li>
     *   <li>APPROACHING_LIMIT if percentageUsed >= warningThreshold</li>
     *   <li>UNDER_BUDGET otherwise</li>
     * </ol>
     */
    TipBudgetStatus determineStatus(int tipCount, BigDecimal percentageUsed, BigDecimal warningThreshold) {
        if (tipCount == 0) {
            return TipBudgetStatus.NO_HISTORY;
        }
        if (percentageUsed.compareTo(HUNDRED) > 0) {
            return TipBudgetStatus.OVER_BUDGET;
        }
        if (percentageUsed.compareTo(HUNDRED) == 0) {
            return TipBudgetStatus.LIMIT_REACHED;
        }
        if (percentageUsed.compareTo(warningThreshold) >= 0) {
            return TipBudgetStatus.APPROACHING_LIMIT;
        }
        return TipBudgetStatus.UNDER_BUDGET;
    }

    /**
     * Determines confidence based on tip count.
     */
    BudgetConfidence determineConfidence(int tipCount) {
        if (tipCount >= 5) {
            return BudgetConfidence.HIGH;
        }
        if (tipCount >= 2) {
            return BudgetConfidence.MEDIUM;
        }
        return BudgetConfidence.LOW;
    }

    /**
     * Generates a deterministic human-readable message.
     */
    private String generateMessage(TipBudgetStatus status, BigDecimal percentageUsed,
                                   BigDecimal remainingBudget, String currency) {
        return switch (status) {
            case NO_HISTORY -> "No tips recorded this month for " + currency + ". Start tipping to track your budget.";
            case UNDER_BUDGET -> String.format("You've used %s%% of your %s budget. %s %s remaining.",
                    percentageUsed.toPlainString(), currency, currency, remainingBudget.toPlainString());
            case APPROACHING_LIMIT -> String.format("Heads up! You've used %s%% of your %s budget. Only %s %s remaining.",
                    percentageUsed.toPlainString(), currency, currency, remainingBudget.toPlainString());
            case LIMIT_REACHED -> String.format("You've reached your %s monthly tipping budget.", currency);
            case OVER_BUDGET -> String.format("You've exceeded your %s budget by %s %s.",
                    currency, currency, remainingBudget.abs().toPlainString());
        };
    }
}
