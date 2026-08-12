package com.aitip.util;

import java.util.Currency;

/**
 * Utility for validating and normalizing ISO 4217 currency codes.
 */
public class CurrencyValidationUtil {

    /**
     * Normalizes a string to a valid ISO 4217 currency code.
     * Example: "usd" -> "USD".
     *
     * @throws IllegalArgumentException if the code is invalid or unsupported.
     */
    public static String normalizeAndValidate(String code) {
        if (code == null || code.trim().length() != 3) {
            throw new IllegalArgumentException("Currency code must be exactly 3 letters.");
        }
        
        String upperCode = code.trim().toUpperCase();
        try {
            // This natively validates against Java's known ISO 4217 list
            Currency.getInstance(upperCode);
            return upperCode;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported or invalid currency code: " + code);
        }
    }
    
    /**
     * Returns the default fraction digits for a given ISO 4217 currency code.
     * Example: "USD" -> 2, "JPY" -> 0
     */
    public static int getFractionDigits(String currencyCode) {
        return Currency.getInstance(currencyCode).getDefaultFractionDigits();
    }
}
