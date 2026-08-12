package com.aitip.dto;

/**
 * DTO for authentication responses (login and registration).
 *
 * <p><b>Purpose:</b> Returns the JWT token and basic user info
 * after successful authentication. Used by both
 * {@code /api/auth/register} and {@code /api/auth/login}.</p>
 *
 * <p><b>Why include name and email?</b>
 * The frontend needs user info immediately after auth to
 * display the dashboard. This avoids an extra API call to
 * {@code /api/users/me} on every login.</p>
 *
 * <p><b>Why tokenType?</b>
 * REST API best practice — tells the client how to use the token
 * (i.e., {@code Authorization: Bearer <token>}).</p>
 *
 * @param token     JWT access token
 * @param tokenType Token type (always "Bearer")
 * @param name      User's display name
 * @param email     User's email address
 */
public record AuthResponse(
        String token,
        String tokenType,
        String name,
        String email
) {
    /**
     * Convenience constructor that defaults tokenType to "Bearer".
     */
    public AuthResponse(String token, String name, String email) {
        this(token, "Bearer", name, email);
    }
}
