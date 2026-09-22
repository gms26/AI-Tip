package com.aitip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) Configuration.
 *
 * <p><b>Purpose:</b> Allows the React frontend (running on localhost:5173)
 * to make API calls to the Spring Boot backend (running on localhost:8080).</p>
 *
 * <p><b>Why a separate config class?</b>
 * <ul>
 *   <li>Single Responsibility: CORS config is separate from security rules</li>
 *   <li>Easy to modify allowed origins for staging/production</li>
 *   <li>Registered as a CorsFilter bean â€” works with Spring Security</li>
 * </ul></p>
 *
 * <p><b>Why allow credentials?</b>
 * The Authorization header with Bearer token requires
 * {@code allowCredentials = true} in CORS.</p>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins — Vite dev server and Docker Nginx
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost"
        ));

        // Allowed HTTP methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allowed request headers
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // Expose the Authorization header in responses
        config.setExposedHeaders(List.of("Authorization"));

        // Allow credentials (cookies, Authorization header)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
