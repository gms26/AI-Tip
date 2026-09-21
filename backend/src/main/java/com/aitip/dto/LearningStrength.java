package com.aitip.dto;

/**
 * Deterministic learning-strength classification based on usable feedback count (Day 35).
 *
 * <ul>
 *   <li>{@link #INSUFFICIENT_DATA}: 0â€“4 usable decisions.</li>
 *   <li>{@link #EARLY_LEARNING}: 5â€“9 usable decisions.</li>
 *   <li>{@link #ESTABLISHED}: 10â€“19 usable decisions.</li>
 *   <li>{@link #STRONG}: 20+ usable decisions.</li>
 * </ul>
 */
public enum LearningStrength {
    INSUFFICIENT_DATA,
    EARLY_LEARNING,
    ESTABLISHED,
    STRONG;

    /**
     * Deterministically classifies the learning strength from a usable-decision count.
     *
     * @param usableDecisionCount number of feedback records with a non-null differencePercentagePoints
     * @return the corresponding strength level
     */
    public static LearningStrength fromCount(int usableDecisionCount) {
        if (usableDecisionCount >= 20) return STRONG;
        if (usableDecisionCount >= 10) return ESTABLISHED;
        if (usableDecisionCount >= 5)  return EARLY_LEARNING;
        return INSUFFICIENT_DATA;
    }
}
