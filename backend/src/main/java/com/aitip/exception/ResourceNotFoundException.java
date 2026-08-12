package com.aitip.exception;

/**
 * Thrown when a requested resource (user, tip, etc.) is not found.
 *
 * <p><b>Purpose:</b> Domain-specific exception for missing resources.
 * Mapped to HTTP 404 (Not Found) by {@link GlobalExceptionHandler}.</p>
 *
 * <p><b>Why extend RuntimeException?</b>
 * Unchecked exceptions don't pollute method signatures with throws
 * clauses. The GlobalExceptionHandler catches them centrally.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
