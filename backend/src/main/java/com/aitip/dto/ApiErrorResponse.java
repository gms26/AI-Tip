package com.aitip.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized API error response envelope.
 *
 * <p><b>Purpose:</b> Provides a consistent error format across
 * all API endpoints. Used by {@link com.aitip.exception.GlobalExceptionHandler}.</p>
 *
 * <p><b>Why a standardized error format?</b>
 * <ul>
 *   <li>Frontend can parse errors consistently</li>
 *   <li>Debugging is easier with timestamp and path</li>
 *   <li>Field-level validation errors are in the {@code errors} map</li>
 *   <li>Follows REST best practices (RFC 7807 inspired)</li>
 * </ul></p>
 *
 * <p><b>Why @JsonInclude(NON_NULL)?</b>
 * Omits null fields from JSON output. For example, a simple
 * 401 error won't include an empty {@code errors} map.</p>
 *
 * @param status    HTTP status code
 * @param message   Human-readable error message
 * @param errors    Field-level validation errors (nullable)
 * @param timestamp When the error occurred
 * @param path      Request URI that caused the error
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        int status,
        String message,
        Map<String, String> errors,
        LocalDateTime timestamp,
        String path
) {
    /**
     * Convenience constructor for errors without field-level details.
     */
    public ApiErrorResponse(int status, String message, String path) {
        this(status, message, null, LocalDateTime.now(), path);
    }

    /**
     * Full constructor with field-level validation errors.
     */
    public ApiErrorResponse(int status, String message, Map<String, String> errors, String path) {
        this(status, message, errors, LocalDateTime.now(), path);
    }
}
