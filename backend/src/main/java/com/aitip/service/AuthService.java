package com.aitip.service;

import com.aitip.dto.AuthResponse;
import com.aitip.dto.LoginRequest;
import com.aitip.dto.RegisterRequest;
import com.aitip.entity.User;
import com.aitip.exception.DuplicateEmailException;
import com.aitip.mapper.UserMapper;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service â€” handles registration and login business logic.
 *
 * <p><b>Purpose:</b> Orchestrates the authentication workflow.
 * Controllers delegate to this service â€” they never contain
 * business logic themselves (SRP).</p>
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Registration: validate uniqueness, encode password, save, generate JWT</li>
 *   <li>Login: authenticate credentials, generate JWT</li>
 * </ul></p>
 *
 * <p><b>Why separate from UserService?</b>
 * Single Responsibility Principle:
 * <ul>
 *   <li>AuthService = authentication workflows (register, login)</li>
 *   <li>UserService = user profile operations (get current user)</li>
 * </ul>
 * They have different reasons to change.</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Constructor injection â€” all 4 dependencies are explicit and final.
     *
     * <p><b>Why 4 dependencies?</b>
     * Each serves a distinct purpose:
     * <ul>
     *   <li>UserRepository: database access</li>
     *   <li>PasswordEncoder: BCrypt hashing</li>
     *   <li>JwtTokenProvider: token generation</li>
     *   <li>AuthenticationManager: credential verification</li>
     * </ul></p>
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registers a new user.
     *
     * <p><b>Flow:</b>
     * <ol>
     *   <li>Check for duplicate email (fail fast)</li>
     *   <li>Map DTO to entity (via UserMapper)</li>
     *   <li>Encode password with BCrypt</li>
     *   <li>Save to database</li>
     *   <li>Generate JWT token</li>
     *   <li>Return AuthResponse</li>
     * </ol></p>
     *
     * <p><b>Why check duplicate before save?</b>
     * Could rely on DB unique constraint, but that throws a
     * generic DataIntegrityViolationException. Checking first
     * allows a clean, user-friendly error message.</p>
     *
     * @param request the registration request
     * @return authentication response with JWT token
     * @throws DuplicateEmailException if email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Check for duplicate email
        if (userRepository.existsByEmail(request.email().toLowerCase().trim())) {
            throw new DuplicateEmailException(
                    "Email '" + request.email() + "' is already registered"
            );
        }

        // 2. Map DTO to entity
        User user = UserMapper.toEntity(request);

        // 3. Encode password â€” NEVER store plaintext
        user.setPassword(passwordEncoder.encode(request.password()));

        // 4. Save to database
        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {}", savedUser.getEmail());

        // 5. Generate JWT
        String token = jwtTokenProvider.generateToken(savedUser.getEmail());

        // 6. Return response
        return UserMapper.toAuthResponse(savedUser, token);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p><b>Flow:</b>
     * <ol>
     *   <li>Authenticate via AuthenticationManager (delegates to UserDetailsService + PasswordEncoder)</li>
     *   <li>Load user entity for response data</li>
     *   <li>Generate JWT token</li>
     *   <li>Return AuthResponse</li>
     * </ol></p>
     *
     * <p><b>Why use AuthenticationManager?</b>
     * It delegates to the configured UserDetailsService and
     * PasswordEncoder. If credentials are invalid, it throws
     * BadCredentialsException â€” caught by GlobalExceptionHandler.</p>
     *
     * @param request the login request
     * @return authentication response with JWT token
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase().trim(),
                        request.password()
                )
        );

        // 2. Load user for response data
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        log.info("User logged in successfully: {}", email);

        // 3. Generate JWT
        String token = jwtTokenProvider.generateToken(email);

        // 4. Return response
        return UserMapper.toAuthResponse(user, token);
    }
}
