package com.aitip.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Token Provider — generates and validates JSON Web Tokens.
 *
 * <p><b>Purpose:</b> Encapsulates all JWT operations: generation,
 * validation, and claim extraction. No other class should directly
 * use the jjwt library.</p>
 *
 * <p><b>Why HMAC-SHA256 (HS256)?</b>
 * <ul>
 *   <li>Symmetric key — simpler than RSA key pairs</li>
 *   <li>Sufficient for single-service architectures</li>
 *   <li>If we add microservices later, switch to RS256</li>
 * </ul></p>
 *
 * <p><b>Why @Component?</b>
 * Needs to be a Spring bean so that @Value injection works.
 * Injected into JwtAuthenticationFilter and AuthService via constructor.</p>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Secret must be at least 256 bits for HS256</li>
 *   <li>In production, externalize to environment variables</li>
 *   <li>Token expiration limits damage from token theft</li>
 * </ul></p>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Constructor injection — the only injection pattern we use.
     *
     * <p><b>Why constructor injection?</b>
     * <ul>
     *   <li>Makes dependencies explicit and final</li>
     *   <li>Enables immutability</li>
     *   <li>Fails fast if a dependency is missing</li>
     *   <li>Easy to unit test with plain constructors</li>
     * </ul></p>
     *
     * @param secret       Base64-encoded JWT secret from application.yml
     * @param expirationMs Token expiration in milliseconds
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {

        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a JWT token for the given email (subject).
     *
     * <p>Token contains:
     * <ul>
     *   <li>Subject: user's email (unique identifier)</li>
     *   <li>Issued At: current timestamp</li>
     *   <li>Expiration: current time + configured expiration</li>
     * </ul></p>
     *
     * @param email the user's email address
     * @return signed JWT token string
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the email (subject) from a JWT token.
     *
     * @param token the JWT token
     * @return the email address stored as subject
     */
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates the JWT token signature and expiration.
     *
     * <p><b>Why catch specific exceptions?</b>
     * Each exception type indicates a different failure mode.
     * Logging them separately aids debugging.</p>
     *
     * @param token the JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Malformed JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}
