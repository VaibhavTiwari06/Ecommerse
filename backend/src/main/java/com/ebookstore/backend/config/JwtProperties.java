package com.ebookstore.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JwtProperties — binds JWT configuration from application.properties.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-FOUND-002, REQ-NFR-001, REQ-NFR-005)
 * =============================================================
 * Spring Boot's @ConfigurationProperties mechanism reads values
 * with the prefix "app.jwt" from application.properties and
 * injects them here as plain Java fields.
 *
 * This is safer than using @Value scattered across multiple
 * classes — all JWT config is in one place.
 *
 * SECURITY NOTE (REQ-NFR-005):
 *   The actual secret value comes from the JWT_SECRET environment
 *   variable (see application.properties: app.jwt.secret=${JWT_SECRET}).
 *   It is NEVER hardcoded in source code or version control.
 *
 * HOW IT WORKS:
 *   application.properties defines:
 *     app.jwt.secret=${JWT_SECRET}        ← from env var
 *     app.jwt.expiration-ms=86400000      ← 24 hours
 *
 *   Spring maps:
 *     app.jwt.secret        → secret field
 *     app.jwt.expiration-ms → expirationMs field
 *   (kebab-case in properties = camelCase in Java, handled automatically)
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * The HS256 signing secret.
     * Loaded from the JWT_SECRET environment variable at runtime.
     * Minimum recommended length: 32 characters for HS256.
     */
    private String secret;

    /**
     * How long a JWT is valid, in milliseconds.
     * Default: 86_400_000 ms = 24 hours.
     */
    private long expirationMs = 86_400_000L;

    // --- Standard getters and setters ---
    // Spring needs setters to inject the values via @ConfigurationProperties.

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
