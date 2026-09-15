package com.aitip.dto;

/**
 * Deterministic confidence level based on tip count in the current month.
 *
 * <ul>
 *   <li>{@code LOW} — 0–1 tips</li>
 *   <li>{@code MEDIUM} — 2–4 tips</li>
 *   <li>{@code HIGH} — 5+ tips</li>
 * </ul>
 */
public enum BudgetConfidence {
    LOW,
    MEDIUM,
    HIGH
}
