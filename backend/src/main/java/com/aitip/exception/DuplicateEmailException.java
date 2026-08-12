package com.aitip.exception;

/**
 * Thrown when a user attempts to register with an email
 * that already exists in the database.
 *
 * <p><b>Purpose:</b> Domain-specific exception for duplicate
 * email conflicts. Mapped to HTTP 409 (Conflict) by
 * {@link GlobalExceptionHandler}.</p>
 *
 * <p><b>Why a custom exception instead of a generic one?</b>
 * <ul>
 *   <li>The GlobalExceptionHandler can map it to the correct HTTP status</li>
 *   <li>Clear intent — anyone reading the code knows exactly what happened</li>
 *   <li>Easy to add additional context (e.g., the duplicate email) if needed</li>
 * </ul></p>
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
