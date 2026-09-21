package com.aitip.service;

import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for Spring Security.
 *
 * <p><b>Purpose:</b> Bridges our User entity with Spring Security's
 * authentication mechanism. Spring Security calls this service
 * during authentication to load user credentials from the database.</p>
 *
 * <p><b>Why implement UserDetailsService?</b>
 * <ul>
 *   <li>Spring Security requires a UserDetailsService to load users</li>
 *   <li>By default, Spring Security uses in-memory users â€” we need DB</li>
 *   <li>This service converts our User entity to Spring's UserDetails</li>
 * </ul></p>
 *
 * <p><b>Why @Transactional(readOnly = true)?</b>
 * Read-only transaction hint allows Hibernate to optimize:
 * <ul>
 *   <li>Skips dirty checking</li>
 *   <li>Can use read replicas if configured</li>
 *   <li>Reduces lock contention</li>
 * </ul></p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructor injection â€” single dependency.
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user details by email (used as username).
     *
     * <p><b>Why email as username?</b>
     * Email is unique and is what users enter during login.
     * More natural than a separate username field.</p>
     *
     * @param email the user's email address
     * @return Spring Security UserDetails
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
