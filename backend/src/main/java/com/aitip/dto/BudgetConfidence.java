package com.aitip.dto;

/**
 * Deterministic confidence level based on tip count in the current month.
 *
 * <ul>
 *   <li>{@code LOW} â€” 0â€“1 tips</li>
 *   <li>{@code MEDIUM} â€” 2â€“4 tips</li>
 *   <li>{@code HIGH} â€” 5+ tips</li>
 * </ul>
 */
public enum BudgetConfidence {
    LOW,
    MEDIUM,
    HIGH
}
