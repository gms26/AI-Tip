package com.aitip.mapper;

import com.aitip.dto.AuthResponse;
import com.aitip.dto.RegisterRequest;
import com.aitip.dto.UserResponse;
import com.aitip.entity.User;

/**
 * Centralized mapping between User entity and DTOs.
 *
 * <p><b>Purpose:</b> Single source of truth for all entity-to-DTO
 * and DTO-to-entity conversions. Prevents mapping logic from
 * scattering across services and controllers.</p>
 *
 * <p><b>Why a utility class instead of MapStruct?</b>
 * <ul>
 *   <li>Only 4 DTOs — MapStruct's annotation processing overhead isn't justified</li>
 *   <li>Fully transparent — no "magic" code generation</li>
 *   <li>Easy to debug and test</li>
 *   <li>Interview-friendly: shows understanding of separation of concerns</li>
 * </ul></p>
 *
 * <p><b>Why static methods?</b>
 * Mappers are stateless transformations. No instance state needed.
 * Making them static avoids unnecessary bean creation.</p>
 */
public final class UserMapper {

    // Prevent instantiation — utility class
    private UserMapper() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    /**
     * Converts a registration request to a User entity.
     *
     * <p><b>NOTE:</b> Password is NOT set here. The service layer
     * is responsible for encoding the password before setting it.
     * This keeps the mapper free of business logic.</p>
     *
     * @param request the registration request DTO
     * @return a User entity (without password or ID)
     */
    public static User toEntity(RegisterRequest request) {
        return User.builder()
                .name(request.name())
                .email(request.email().toLowerCase().trim())
                .build();
    }

    /**
     * Converts a User entity to a safe response DTO.
     * Password is explicitly excluded.
     *
     * @param user the User entity
     * @return a UserResponse DTO (no sensitive data)
     */
    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    /**
     * Creates an authentication response with the JWT token
     * and basic user info.
     *
     * @param user  the authenticated User entity
     * @param token the generated JWT token
     * @return an AuthResponse DTO
     */
    public static AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                token,
                user.getName(),
                user.getEmail()
        );
    }
}
