package com.aitip.security;

import com.aitip.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — intercepts every HTTP request.
 *
 * <p><b>Purpose:</b> Extracts the JWT token from the Authorization
 * header, validates it, and sets the authenticated user in the
 * Spring Security context. Runs ONCE per request.</p>
 *
 * <p><b>Why OncePerRequestFilter?</b>
 * Guarantees the filter executes exactly once per request,
 * even with request forwarding or error dispatches.</p>
 *
 * <p><b>Flow:</b>
 * <ol>
 *   <li>Extract Bearer token from Authorization header</li>
 *   <li>Validate token signature and expiration</li>
 *   <li>Load UserDetails from database</li>
 *   <li>Set authentication in SecurityContext</li>
 *   <li>Continue the filter chain</li>
 * </ol></p>
 *
 * <p><b>Why load from DB instead of trusting token claims?</b>
 * Defense-in-depth: if a user is deleted or disabled after token
 * issuance, the DB lookup will fail, preventing access.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Constructor injection — both dependencies are final.
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String email = jwtTokenProvider.getEmailFromToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // No credentials needed — already authenticated via JWT
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set authentication in the security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated user: {}", email);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        // Always continue the filter chain — even if auth fails.
        // Spring Security will handle 401/403 for protected endpoints.
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT from the Authorization header.
     *
     * <p><b>Why this pattern?</b>
     * RFC 6750 specifies Bearer token format:
     * {@code Authorization: Bearer <token>}</p>
     *
     * @param request the HTTP request
     * @return the JWT string, or null if not present
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
