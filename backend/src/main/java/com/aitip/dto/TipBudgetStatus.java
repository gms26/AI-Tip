package com.aitip.dto;

/**
 * Deterministic budget status based on percentage used vs warning threshold.
 *
 * <p>Evaluation order (highest priority first):
 * <ol>
 *   <li>{@code NO_HISTORY} â€” tipCount == 0</li>
 *   <li>{@code OVER_BUDGET} â€” percentageUsed > 100</li>
 *   <li>{@code LIMIT_REACHED} â€” percentageUsed == 100</li>
 *   <li>{@code APPROACHING_LIMIT} â€” percentageUsed >= warningThreshold</li>
 *   <li>{@code UNDER_BUDGET} â€” otherwise</li>
 * </ol></p>
 */
public enum TipBudgetStatus {
    UNDER_BUDGET,
    APPROACHING_LIMIT,
    LIMIT_REACHED,
    OVER_BUDGET,
    NO_HISTORY
}
