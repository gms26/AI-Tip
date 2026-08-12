package com.aitip.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user profile responses.
 *
 * <p><b>Purpose:</b> Returns safe user information to the client.
 * Used by {@code GET /api/users/me}.</p>
 *
 * <p><b>Why no password field?</b>
 * CRITICAL SECURITY RULE: Passwords (even hashed) must NEVER
 * leave the server. This DTO explicitly excludes it.
 * This is why we never expose entities directly.</p>
 *
 * <p><b>Why include createdAt?</b>
 * Useful for "Member since" display on the frontend.
 * updatedAt is an internal audit field — not exposed.</p>
 *
 * @param id        User's unique identifier
 * @param name      User's display name
 * @param email     User's email address
 * @param createdAt When the user registered
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        LocalDateTime createdAt
) {
}
