package com.aitip.exception;

/**
 * Thrown when the currency conversion service fails (e.g., provider timeout, malformed response).
 * Caught by GlobalExceptionHandler to return a 503.
 */
public class CurrencyServiceException extends RuntimeException {
    
    public CurrencyServiceException(String message) {
        super(message);
    }

    public CurrencyServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
