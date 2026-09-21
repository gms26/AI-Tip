package com.aitip.exception;

import com.aitip.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 *
 * <p><b>Purpose:</b> Single source of truth for error response formatting.
 * Every exception thrown by controllers or services is caught here and
 * converted to a consistent {@link ApiErrorResponse}.</p>
 *
 * <p><b>Why @RestControllerAdvice?</b>
 * <ul>
 *   <li>Eliminates try-catch blocks in controllers (SRP)</li>
 *   <li>Guarantees consistent error format across ALL endpoints</li>
 *   <li>Centralizes logging for all error scenarios</li>
 *   <li>Maps domain exceptions to correct HTTP status codes</li>
 * </ul></p>
 *
 * <p><b>Why log at different levels?</b>
 * <ul>
 *   <li>WARN for client errors (4xx) â€” expected scenarios</li>
 *   <li>ERROR for server errors (5xx) â€” unexpected failures</li>
 * </ul></p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation failures (e.g., @NotBlank, @Email, @Size).
     * Returns field-level error details so the frontend can display
     * per-field validation messages.
     *
     * <p><b>HTTP 400 Bad Request</b></p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed for {}: {}", request.getRequestURI(), fieldErrors);

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                fieldErrors,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles invalid split calculations.
     *
     * <p><b>HTTP 400 Bad Request</b></p>
     */
    @ExceptionHandler(InvalidSplitException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSplit(
            InvalidSplitException ex,
            HttpServletRequest request) {

        log.warn("Invalid split request: {}", ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles AI service failures (Groq timeouts, rate limits, malformed JSON).
     *
     * <p><b>HTTP 503 Service Unavailable</b></p>
     * <p>We do NOT expose the raw AI error message to the client. We log it
     * internally and return a safe, generic fallback message.</p>
     */
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleAiServiceException(
            AiServiceException ex,
            HttpServletRequest request) {

        log.error("AI Service failed: {}", ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI recommendation is temporarily unavailable. You can still calculate your tip manually.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles Currency service failures (e.g. Frankfurter API timeouts or malformed responses).
     *
     * <p><b>HTTP 503 Service Unavailable</b></p>
     */
    @ExceptionHandler(CurrencyServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleCurrencyServiceException(
            CurrencyServiceException ex,
            HttpServletRequest request) {

        log.error("Currency Service failed: {}", ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Currency conversion is temporarily unavailable.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles invalid login credentials.
     *
     * <p><b>Why a generic message?</b>
     * Never reveal whether the email or password was wrong.
     * This prevents user enumeration attacks.</p>
     *
     * <p><b>HTTP 401 Unauthorized</b></p>
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Bad credentials attempt for: {}", request.getRequestURI());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid email or password",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles duplicate email registration attempts.
     *
     * <p><b>HTTP 409 Conflict</b></p>
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex,
            HttpServletRequest request) {

        log.warn("Duplicate email registration attempt: {}", ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles missing resource lookups.
     *
     * <p><b>HTTP 404 Not Found</b></p>
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles bad arguments (e.g. invalid currency codes from CurrencyValidationUtil).
     *
     * <p><b>HTTP 400 Bad Request</b></p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles JSON parsing errors, such as invalid enum values.
     *
     * <p><b>HTTP 400 Bad Request</b></p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed JSON request at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON request or invalid enum value.",
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatchException(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        log.warn("Type mismatch at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid parameter type or value.",
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {
        log.warn("Illegal state at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ReceiptOcrException.class)
    public ResponseEntity<ApiErrorResponse> handleReceiptOcrException(
            ReceiptOcrException ex,
            HttpServletRequest request) {
        log.warn("Receipt OCR failed: {}", ex.getMessage());
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ReceiptOcrUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleReceiptOcrUnavailableException(
            ReceiptOcrUnavailableException ex,
            HttpServletRequest request) {
        log.error("Receipt OCR unavailable: {}", ex.getMessage());
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Catch-all for unexpected exceptions.
     *
     * <p><b>Why not expose the actual error message?</b>
     * Internal details (stack traces, SQL errors) could leak
     * sensitive information. Log the full error, return a generic message.</p>
     *
     * <p><b>HTTP 500 Internal Server Error</b></p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
