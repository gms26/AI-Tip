package com.aitip.exception;

/**
 * Thrown when a split calculation request is invalid.
 * For example: trying to split among fewer than 2 people,
 * or custom split amounts that don't add up to the total.
 *
 * <p>Mapped to HTTP 400 (Bad Request) by GlobalExceptionHandler.</p>
 */
public class InvalidSplitException extends RuntimeException {
    public InvalidSplitException(String message) {
        super(message);
    }
}
