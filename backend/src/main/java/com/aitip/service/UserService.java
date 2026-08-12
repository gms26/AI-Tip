package com.aitip.service;

import com.aitip.dto.UserResponse;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.mapper.UserMapper;
import com.aitip.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service — handles user profile operations.
 *
 * <p><b>Purpose:</b> Provides user profile data to authorized clients.
 * Separated from AuthService following SRP — this service handles
 * "who am I?" questions, not "can I log in?" questions.</p>
 *
 * <p><b>Why a separate service?</b>
 * <ul>
 *   <li>AuthService will never need user profile logic</li>
 *   <li>UserService will grow with profile updates, preferences, etc.</li>
 *   <li>Different reasons to change = different classes (SRP)</li>
 * </ul></p>
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    /**
     * Constructor injection — single dependency.
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the current user's profile by email.
     *
     * <p><b>Why email parameter?</b>
     * The controller extracts the authenticated email from
     * SecurityContext and passes it here. The service doesn't
     * depend on SecurityContext directly — easier to test.</p>
     *
     * @param email the authenticated user's email
     * @return the user's profile (without password)
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = getUserByEmail(email);
        return UserMapper.toUserResponse(user);
    }

    /**
     * Retrieves the current user entity by email for internal service use.
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email
                ));
    }
}
