package com.aitip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration requests.
 *
 * <p><b>Purpose:</b> Captures and validates registration input.
 * Used as the request body for {@code POST /api/auth/register}.</p>
 *
 * <p><b>Why a Record?</b>
 * <ul>
 *   <li>Immutable â€” request data should never be mutated after deserialization</li>
 *   <li>Concise â€” auto-generates constructor, getters, equals, hashCode, toString</li>
 *   <li>No Lombok needed â€” Records are a Java 16+ language feature</li>
 * </ul></p>
 *
 * <p><b>Why Jakarta Validation annotations here?</b>
 * Validation belongs at the DTO boundary â€” fail fast before
 * reaching the service layer. The controller uses {@code @Valid}
 * to trigger validation automatically.</p>
 *
 * @param name     User's display name (required)
 * @param email    User's email address (must be valid format)
 * @param password User's password (minimum 8 characters for security)
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {
}
