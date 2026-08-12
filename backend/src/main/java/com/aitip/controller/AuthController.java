package com.aitip.controller;

import com.aitip.dto.AuthResponse;
import com.aitip.dto.LoginRequest;
import com.aitip.dto.RegisterRequest;
import com.aitip.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller — handles registration and login endpoints.
 *
 * <p><b>Purpose:</b> Thin REST controller that validates input,
 * delegates to AuthService, and returns responses. Contains
 * ZERO business logic — only request/response handling.</p>
 *
 * <p><b>Why @Valid?</b>
 * Triggers Jakarta Bean Validation on the request DTOs.
 * If validation fails, Spring throws MethodArgumentNotValidException,
 * which is caught by GlobalExceptionHandler and returns a 400
 * with field-level error details.</p>
 *
 * <p><b>Why ResponseEntity?</b>
 * Explicit control over HTTP status codes, headers, and body.
 * More expressive than relying on default 200 OK.</p>
 *
 * <p><b>REST Standards:</b>
 * <ul>
 *   <li>POST /api/auth/register → 201 Created (new resource)</li>
 *   <li>POST /api/auth/login → 200 OK (existing resource action)</li>
 * </ul></p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructor injection — single dependency.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     *
     * <p><b>Flow:</b>
     * Request → @Valid → AuthService.register() → 201 + AuthResponse</p>
     *
     * <p><b>Why 201 Created?</b>
     * REST convention: POST that creates a new resource returns 201,
     * not 200. Signals to the client that a resource was created.</p>
     *
     * @param request validated registration data
     * @return JWT token and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login an existing user.
     *
     * <p><b>Flow:</b>
     * Request → @Valid → AuthService.login() → 200 + AuthResponse</p>
     *
     * <p><b>Why 200 OK?</b>
     * Login doesn't create a resource — it authenticates
     * and returns a token. 200 is the correct status.</p>
     *
     * @param request validated login credentials
     * @return JWT token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
