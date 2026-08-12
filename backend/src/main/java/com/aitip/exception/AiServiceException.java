package com.aitip.exception;

/**
 * Thrown when the AI recommendation engine fails.
 * 
 * <p>This encompasses Gemini API timeouts, rate limits, network errors,
 * or scenarios where Gemini returns an unparseable/invalid JSON response.</p>
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }
    
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
