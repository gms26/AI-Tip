package com.aitip.dto;

/**
 * Deterministic personalization-effect classification (Day 35).
 *
 * <p>Compares the absolute average deviation of the earlier half of decisions
 * with the recent half to determine whether personalization is improving.</p>
 *
 * <ul>
 *   <li>{@link #IMPROVING}: Recent decisions have meaningfully smaller absolute deviation.</li>
 *   <li>{@link #STABLE}: The difference between earlier and recent deviation is small.</li>
 *   <li>{@link #DIVERGING}: Recent decisions have meaningfully larger absolute deviation.</li>
 *   <li>{@link #INSUFFICIENT_DATA}: Not enough chronological data (fewer than 4 usable decisions).</li>
 * </ul>
 */
public enum PersonalizationEffect {
    IMPROVING,
    STABLE,
    DIVERGING,
    INSUFFICIENT_DATA
}
