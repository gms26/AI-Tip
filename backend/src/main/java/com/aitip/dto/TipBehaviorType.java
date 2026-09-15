package com.aitip.dto;

/**
 * Deterministic behavior classification based on consistency score.
 *
 * <p><b>Thresholds (documented, immutable):</b></p>
 * <ul>
 *   <li>90–100 → VERY_CONSISTENT</li>
 *   <li>70–89  → CONSISTENT</li>
 *   <li>40–69  → VARIABLE</li>
 *   <li>0–39   → HIGHLY_VARIABLE</li>
 * </ul>
 *
 * <p>Gemini may NOT alter these boundaries.</p>
 */
public enum TipBehaviorType {
    VERY_CONSISTENT,
    CONSISTENT,
    VARIABLE,
    HIGHLY_VARIABLE
}
