package com.aitip.dto;

/**
 * Deterministic feedback classification for Smart Tip recommendations (Day 32).
 *
 * <ul>
 *   <li>{@link #ACCEPTED}: The user explicitly applies the assistant's recommendation and saves that exact percentage.</li>
 *   <li>{@link #MODIFIED}: The user starts from an assistant recommendation but saves a different percentage.</li>
 *   <li>{@link #CUSTOM}: The user saves a tip without applying an assistant suggestion.</li>
 * </ul>
 */
public enum SmartTipFeedbackType {
    ACCEPTED,
    MODIFIED,
    CUSTOM
}
