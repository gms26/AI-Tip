package com.aitip.dto;

/**
 * Deterministic behavior classification based on consistency score.
 *
 * <p><b>Thresholds (documented, immutable):</b></p>
 * <ul>
 *   <li>90â€“100 â†’ VERY_CONSISTENT</li>
 *   <li>70â€“89  â†’ CONSISTENT</li>
 *   <li>40â€“69  â†’ VARIABLE</li>
 *   <li>0â€“39   â†’ HIGHLY_VARIABLE</li>
 * </ul>
 *
 * <p>Groq may NOT alter these boundaries.</p>
 */
public enum TipBehaviorType {
    VERY_CONSISTENT,
    CONSISTENT,
    VARIABLE,
    HIGHLY_VARIABLE
}
