package com.ebookstore.backend.auth;

import com.ebookstore.backend.config.JwtProperties;
import com.ebookstore.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtService — creates and validates JSON Web Tokens.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-003, TASK-AUTH-004, REQ-NFR-001)
 * =============================================================
 * A JWT (JSON Web Token) is a signed, self-contained token that
 * the client stores and sends with every request in the
 * "Authorization: Bearer <token>" header.
 *
 * The token has three parts (base64-encoded, dot-separated):
 *   HEADER.PAYLOAD.SIGNATURE
 *
 * Our token payload (claims) contains:
 *   sub  — the user's numeric ID (subject)
 *   email — the user's email (for convenience)
 *   iat  — issued-at timestamp
 *   exp  — expiry timestamp (24 hours from issue)
 *
 * HOW SIGNING WORKS:
 *   We sign with HMAC-SHA256 (HS256) using a secret key loaded from
 *   the JWT_SECRET environment variable. Anyone with the secret can
 *   verify the token. This is why the secret must be kept secure
 *   and never committed to source control (REQ-NFR-005).
 *
 * JJWT LIBRARY:
 *   We use io.jsonwebtoken:jjwt-api v0.12.6.
 *   Jwts.builder()  — creates tokens
 *   Jwts.parser()   — validates and reads tokens
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** JWT configuration (secret + expiry) loaded from application.properties. */
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    // ------------------------------------------------------------------
    // Token Generation
    // ------------------------------------------------------------------

    /**
     * Generates a signed JWT for the given user.
     *
     * The token contains:
     *   sub   → user's id as a string (standard JWT subject claim)
     *   email → user's email address
     *   iat   → current time (issued-at)
     *   exp   → current time + expirationMs (expiry)
     *
     * @param user the authenticated user
     * @return a signed JWT string, e.g. "eyJhbGci...xxxxx"
     */
    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                // sub = user ID (as string per JWT spec)
                .subject(String.valueOf(user.getId()))
                // custom claim: email stored for convenience
                .claim("email", user.getEmail())
                // timestamps
                .issuedAt(now)
                .expiration(expiry)
                // sign with our HS256 key derived from JWT_SECRET
                .signWith(getSigningKey())
                .compact();
    }

    // ------------------------------------------------------------------
    // Token Validation
    // ------------------------------------------------------------------

    /**
     * Validates a JWT string and returns its claims payload.
     *
     * Validation checks performed by JJWT:
     *   1. Signature is valid (token was not tampered with).
     *   2. Token has not expired (exp > now).
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return the Claims payload if valid
     * @throws JwtException if the token is invalid or expired
     */
    public Claims validateAndExtractClaims(String token) {
        // parseSignedClaims throws JwtException (and subtypes) for any
        // invalid token — expired, wrong signature, malformed, etc.
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user ID from a validated JWT's subject claim.
     *
     * @param token the raw JWT string
     * @return the user's Long ID
     */
    public Long extractUserId(String token) {
        Claims claims = validateAndExtractClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Checks if a token is valid (signature good + not expired).
     * Returns false instead of throwing for use in filter logic.
     *
     * @param token the raw JWT string
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            validateAndExtractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Key helper
    // ------------------------------------------------------------------

    /**
     * Derives an HMAC-SHA256 signing key from the configured secret string.
     *
     * Keys.hmacShaKeyFor() requires the key bytes to be long enough for
     * HS256 (at least 32 bytes / 256 bits). Our secret from JWT_SECRET
     * must meet this requirement — enforced by application startup.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret()
                .getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
