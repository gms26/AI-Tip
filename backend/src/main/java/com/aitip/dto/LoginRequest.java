package com.aitip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login requests.
 *
 * <p><b>Purpose:</b> Captures and validates login credentials.
 * Used as the request body for {@code POST /api/auth/login}.</p>
 *
 * <p><b>Why no @Size on password here?</b>
 * Login should not reveal password policy. If the password
 * is wrong, return a generic "Bad credentials" error.
 * Validation constraints on password length belong only
 * in {@link RegisterRequest}.</p>
 *
 * @param email    User's email address
 * @param password User's password
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
