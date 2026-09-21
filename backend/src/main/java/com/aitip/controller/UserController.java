package com.aitip.controller;

import com.aitip.dto.UserResponse;
import com.aitip.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User Controller â€” handles user profile endpoints.
 *
 * <p><b>Purpose:</b> Provides authenticated user profile data.
 * All endpoints in this controller require a valid JWT token.</p>
 *
 * <p><b>Why separate from AuthController?</b>
 * <ul>
 *   <li>AuthController = public endpoints (register, login)</li>
 *   <li>UserController = protected endpoints (user profile)</li>
 *   <li>Different security requirements = different controllers</li>
 * </ul></p>
 *
 * <p><b>Why inject Authentication instead of @AuthenticationPrincipal?</b>
 * Authentication.getName() returns the username (email) set by
 * the JWT filter. Simpler and doesn't require a custom principal.</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * Constructor injection.
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get the currently authenticated user's profile.
     *
     * <p><b>Security:</b> This endpoint is protected â€” requires
     * a valid JWT token in the Authorization header.</p>
     *
     * <p><b>Flow:</b>
     * JWT Filter â†’ SecurityContext â†’ Authentication â†’ email â†’ UserService â†’ UserResponse</p>
     *
     * @param authentication injected by Spring Security from SecurityContext
     * @return the user's profile (without password)
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserResponse user = userService.getCurrentUser(email);
        return ResponseEntity.ok(user);
    }
}
