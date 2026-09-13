package com.ebookstore.backend.config;

import com.ebookstore.backend.auth.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * SecurityConfig — Spring Security configuration for the entire application.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-FOUND-003, REQ-NFR-001, REQ-USR-005)
 * =============================================================
 * Spring Security intercepts every incoming HTTP request before it
 * reaches any controller. This class configures:
 *
 *  1. Which endpoints are PUBLIC (no JWT required)
 *  2. Which endpoints are PROTECTED (JWT required)
 *  3. That the app is STATELESS — no HTTP sessions, JWT only
 *  4. CSRF is disabled — not needed for stateless REST APIs
 *  5. BCrypt password encoding strength
 *
 * PUBLIC endpoints (REQ-USR-001, REQ-CAT-001, design §3.4):
 *   /api/auth/**        — register and login
 *   /api/books/**       — catalogue browsing (guest access allowed)
 *   /api/categories     — category list
 *   /api/publishers     — publisher list
 *   /actuator/health    — health check
 *
 * PROTECTED endpoints (everything else):
 *   /api/cart/**        — requires login (REQ-CRT-001)
 *   /api/orders/**      — requires login (REQ-ORD-002)
 *   /api/checkout/**    — requires login (REQ-CHK-001)
 *   /api/payment/**     — requires login (REQ-PAY-001)
 *   /api/user/**        — requires login (REQ-USR-006)
 *   /api/recommendations/** — requires login (REQ-REC-001)
 *
 * NOTE: The JwtAuthenticationFilter (TASK-AUTH-004 in M2) is not yet
 * wired in here — it will be added when M2 is implemented.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * The CORS configuration built in CorsConfig.
     * Spring Security needs a reference to it so it can apply CORS
     * rules before checking authentication.
     */
    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Defines the HTTP security rules — the heart of this configuration.
     *
     * The filter chain processes every request in this order:
     *   CORS → CSRF(disabled) → StatelessSession → AuthorizationRules
     *
     * @param http the Spring Security HTTP builder
     * @return the configured SecurityFilterChain bean
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Apply our CORS rules (defined in CorsConfig)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Disable CSRF — REST APIs using JWT tokens don't need CSRF
            // protection because there are no browser-managed session cookies
            .csrf(AbstractHttpConfigurer::disable)

            // STATELESS — Spring Security must NOT create or use HTTP sessions.
            // Authentication is entirely token-based (JWT on every request).
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules — evaluated in order, first match wins
            .authorizeHttpRequests(auth -> auth

                // ---- PUBLIC endpoints — no JWT required ----
                .requestMatchers(
                    "/api/auth/**",         // register + login
                    "/api/books/**",        // catalogue (guest access, REQ-USR-001)
                    "/api/categories",      // category list
                    "/api/publishers",      // publisher list
                    "/actuator/health"      // health check
                ).permitAll()

                // ---- Everything else requires a valid JWT ----
                    .anyRequest().authenticated()
                )
                // Return 401 (not 403) for unauthenticated requests.
                // Spring Security 7 defaults to 403 when no entry point is set.
                // We explicitly set a 401 entry point so clients know they need
                // to send a JWT token (REQ-NFR-001, REQ-USR-005).
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(
                            jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                            "Authentication required"))
                );
    
            // Add the JWT filter BEFORE Spring Security's default username/password
            // authentication filter. This ensures every request is checked for a
            // valid JWT token before any other authentication processing occurs.
            http.addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);
    
            return http.build();
    }

    /**
     * BCryptPasswordEncoder bean (REQ-NFR-001, REQ-USR-004).
     *
     * BCrypt is a one-way hashing algorithm designed for passwords.
     * Strength 12 means 2^12 = 4096 hashing rounds, making brute-force
     * attacks computationally expensive.
     *
     * This bean is injected into AuthService (M2) for hashing and
     * verifying passwords. Passwords are NEVER stored in plain text.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationManager bean — needed by AuthService (M2) to
     * programmatically authenticate a username/password pair during login.
     *
     * @param config Spring's authentication configuration
     * @return the default AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
