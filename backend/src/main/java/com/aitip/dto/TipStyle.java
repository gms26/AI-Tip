package com.aitip.dto;

/**
 * Tipping style classification, reusing the Day 8 generosity score categories.
 *
 * <p><b>Category boundaries (from Day 8 GenerosityScoreService):</b></p>
 * <ul>
 *   <li>0–39   → CONSERVATIVE</li>
 *   <li>40–59  → MODERATE</li>
 *   <li>60–79  → GENEROUS</li>
 *   <li>80–100 → VERY_GENEROUS</li>
 * </ul>
 *
 * <p>These must remain consistent with the existing generosity score logic.
 * Do not create a second incompatible generosity classification.</p>
 */
public enum TipStyle {
    CONSERVATIVE,
    MODERATE,
    GENEROUS,
    VERY_GENEROUS
}
