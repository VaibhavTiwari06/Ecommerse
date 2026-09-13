package com.ebookstore.backend.auth;

import com.ebookstore.backend.user.User;
import com.ebookstore.backend.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JwtAuthenticationFilter — intercepts every HTTP request and validates JWT.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-004, REQ-NFR-001, REQ-USR-005)
 * =============================================================
 * This filter runs ONCE per request (extends OncePerRequestFilter)
 * and sits in the Spring Security filter chain BEFORE the standard
 * authentication processing.
 *
 * WHAT IT DOES FOR EACH REQUEST:
 *
 *  1. Reads the "Authorization" header.
 *  2. If it starts with "Bearer ", extracts the token string.
 *  3. Validates the token using JwtService.
 *  4. If valid, loads the User from the DB, sets up a Spring Security
 *     Authentication object in the SecurityContext.
 *  5. If invalid/missing, does nothing — Spring Security's authorization
 *     rules will reject the request with 401 if the endpoint is protected.
 *
 * WHY SET THE SecurityContext?
 *   Spring Security checks the SecurityContext to decide if a request
 *   is authenticated. By setting an Authentication object here, we tell
 *   Spring "this request is authenticated as user X" — protected
 *   endpoints then allow it through.
 *
 * WHY ONLY "Bearer " TOKENS?
 *   The API design specifies "Authorization: Bearer <token>" as the
 *   authentication scheme. This is the standard for JWT-based REST APIs.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** Prefix that all JWT Authorization headers must start with. */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Core filter logic — called once per request.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain to continue processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // --- Step 1: Extract the token from the Authorization header ---

        String authHeader = request.getHeader("Authorization");

        // If no Authorization header or it doesn't start with "Bearer ",
        // skip token processing — Spring Security handles the rest
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip the "Bearer " prefix to get the raw token string
        String token = authHeader.substring(BEARER_PREFIX.length());

        // --- Step 2: Validate the token ---

        try {
            Long userId = jwtService.extractUserId(token);

            // Only proceed if no authentication is already set
            // (avoids re-processing on the same request)
            if (userId != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // --- Step 3: Load user from DB ---
                Optional<User> userOpt = userRepository.findById(userId);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    // --- Step 4: Build Spring Security Authentication object ---
                    //
                    // UsernamePasswordAuthenticationToken is the standard
                    // way to represent an authenticated principal in Spring Security.
                    //
                    // Parameters:
                    //   principal   — the User object (accessible later via
                    //                 SecurityContextHolder.getContext()
                    //                 .getAuthentication().getPrincipal())
                    //   credentials — null (we don't need the password here)
                    //   authorities — empty list (we're not using roles yet)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    Collections.emptyList()
                            );

                    // Attach request details (IP, session) to the auth object
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    // --- Step 5: Set authentication in SecurityContext ---
                    // This tells Spring Security the request is authenticated
                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    log.debug("Authenticated user id={} for request {}",
                            userId, request.getRequestURI());
                }
            }

        } catch (JwtException e) {
            // Token is invalid or expired — log at debug level (expected)
            // Don't set authentication — Spring Security will return 401
            log.debug("JWT validation failed for request {}: {}",
                    request.getRequestURI(), e.getMessage());
        }

        // Continue the filter chain regardless — the authorization
        // rules in SecurityConfig will decide if the request is allowed
        filterChain.doFilter(request, response);
    }
}
