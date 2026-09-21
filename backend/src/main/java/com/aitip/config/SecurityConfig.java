package com.aitip.config;

import com.aitip.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration.
 *
 * <p><b>Purpose:</b> Configures the security filter chain for
 * JWT-based stateless authentication.</p>
 *
 * <p><b>Key Decisions:</b>
 * <ul>
 *   <li><b>CSRF disabled:</b> Stateless JWT APIs don't use cookies,
 *       so CSRF protection is unnecessary and would block POST requests</li>
 *   <li><b>STATELESS sessions:</b> No server-side session state.
 *       Every request is authenticated independently via JWT</li>
 *   <li><b>JWT filter placement:</b> Runs BEFORE UsernamePasswordAuthenticationFilter
 *       to set SecurityContext before Spring Security checks authorization</li>
 *   <li><b>BCrypt:</b> Industry-standard adaptive hashing algorithm.
 *       Cost factor automatically increases computation over time</li>
 * </ul></p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Constructor injection.
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the security filter chain.
     *
     * <p><b>Authorization Rules:</b>
     * <ul>
     *   <li>{@code /api/auth/**} â€” PUBLIC (register, login)</li>
     *   <li>{@code /health} â€” PUBLIC (health check)</li>
     *   <li>Everything else â€” AUTHENTICATED</li>
     * </ul></p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF â€” stateless API, no cookies
                .csrf(AbstractHttpConfigurer::disable)
                // Return 401 Unauthorized for unauthenticated requests instead of 403 Forbidden
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
                )

                // Stateless session â€” no server-side session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .anyRequest().authenticated()
                )

                // Add JWT filter before Spring's default auth filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * BCrypt password encoder bean.
     *
     * <p><b>Why BCrypt?</b>
     * <ul>
     *   <li>Adaptive: cost factor makes brute-force harder over time</li>
     *   <li>Salted: each hash includes a unique random salt</li>
     *   <li>Industry standard for password storage</li>
     *   <li>Default strength (10 rounds) is a good balance</li>
     * </ul></p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager bean.
     *
     * <p><b>Why expose as a bean?</b>
     * The AuthService needs it to authenticate login credentials.
     * Spring Boot auto-configures it, but we need to expose it
     * explicitly for injection.</p>
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
