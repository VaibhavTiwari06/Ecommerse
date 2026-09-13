package com.ebookstore.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CorsConfig — Cross-Origin Resource Sharing configuration.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-FOUND-003, REQ-NFR-001)
 * =============================================================
 * CORS is a browser security mechanism. When our React frontend
 * (running on http://localhost:5173 in development, or a different
 * domain in production) sends an HTTP request to our Spring Boot
 * backend (http://localhost:8080), the browser first sends a
 * "preflight" OPTIONS request asking "is this origin allowed?".
 *
 * Without this config, the browser would block all cross-origin
 * requests from the frontend to the backend.
 *
 * WHAT WE ALLOW:
 *   - Origins: localhost:5173 (dev) + the deployed FRONTEND_URL env var
 *   - Methods: GET, POST, PUT, DELETE, OPTIONS
 *   - Headers: all (the JWT Authorization header must be allowed)
 *   - Credentials: true (needed to send the Authorization header)
 *
 * SECURITY NOTE:
 *   We do NOT use allowedOrigins("*") which would allow any website
 *   to call our API. We only allow our known frontend origins.
 */
@Configuration
public class CorsConfig {

    /**
     * The deployed frontend URL injected from the FRONTEND_URL environment
     * variable. Falls back to localhost:5173 if not set (local development).
     */
    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Registers a CorsConfigurationSource bean.
     * Spring Security picks this up automatically in SecurityConfig
     * when we call: cors(cors -> cors.configurationSource(corsConfigurationSource))
     *
     * @return configured CORS rules scoped to all /api/** paths
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Only our own frontend origins are allowed — not a wildcard
        config.setAllowedOrigins(List.of("http://localhost:5173", frontendUrl));

        // These are the HTTP methods the frontend will use
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers — this is needed so the browser can send
        // the "Authorization: Bearer <token>" header
        config.setAllowedHeaders(List.of("*"));

        // Must be true so browsers send the Authorization header with requests
        config.setAllowCredentials(true);

        // Apply these CORS rules only to our /api/** endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
