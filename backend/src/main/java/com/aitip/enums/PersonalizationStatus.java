package com.aitip.enums;

/**
 * Represents the personalization control state for Smart Tip recommendations (Day 36).
 *
 * <p>Two states only:
 * <ul>
 *   <li>{@link #ENABLED} â€” feedback-based adaptation is active</li>
 *   <li>{@link #DISABLED} â€” baseline deterministic recommendations only</li>
 * </ul></p>
 */
public enum PersonalizationStatus {
    ENABLED,
    DISABLED
}
