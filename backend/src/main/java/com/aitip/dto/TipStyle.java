package com.aitip.dto;

/**
 * Tipping style classification, reusing the Day 8 generosity score categories.
 *
 * <p><b>Category boundaries (from Day 8 GenerosityScoreService):</b></p>
 * <ul>
 *   <li>0â€“39   â†’ CONSERVATIVE</li>
 *   <li>40â€“59  â†’ MODERATE</li>
 *   <li>60â€“79  â†’ GENEROUS</li>
 *   <li>80â€“100 â†’ VERY_GENEROUS</li>
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
