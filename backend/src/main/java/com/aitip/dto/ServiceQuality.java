package com.aitip.dto;

/**
 * Standardized levels of service quality for AI evaluation.
 *
 * <p>Using an enum prevents users from passing arbitrary strings 
 * (like "terrible" or "amazing") and gives the AI a consistent 
 * scale to work with.</p>
 */
public enum ServiceQuality {
    POOR,
    AVERAGE,
    GOOD,
    EXCELLENT
}
