package com.aitip.dto;

/**
 * Behavioral preference direction based on accumulated recommendation feedback (Day 32).
 *
 * <ul>
 *   <li>{@link #PREFERS_HIGHER}: User consistently saves higher percentages than recommended (>= +2.0 pp).</li>
 *   <li>{@link #PREFERS_LOWER}: User consistently saves lower percentages than recommended (<= -2.0 pp).</li>
 *   <li>{@link #ALIGNED}: User's choices are generally aligned with recommendations (between -2.0 and +2.0 pp).</li>
 *   <li>{@link #INSUFFICIENT_DATA}: Insufficient feedback evidence (&lt; 5 records) to determine a preference.</li>
 * </ul>
 */
public enum SmartTipFeedbackDirection {
    PREFERS_HIGHER,
    PREFERS_LOWER,
    ALIGNED,
    INSUFFICIENT_DATA
}
