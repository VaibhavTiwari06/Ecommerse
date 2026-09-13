package com.ebookstore.backend.auth;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.LoginRequestDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController — handles authentication endpoints.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-002, TASK-AUTH-003, REQ-USR-004, REQ-USR-005)
 * =============================================================
 * Two endpoints (both public — no JWT required, see SecurityConfig):
 *
 *  POST /api/auth/register  — creates a new account, returns JWT
 *  POST /api/auth/login     — validates credentials, returns JWT
 *
 * CONTROLLER RESPONSIBILITIES (minimal by design):
 *   1. Receive the HTTP request.
 *   2. Trigger Bean Validation via @Valid.
 *   3. Delegate all business logic to AuthService.
 *   4. Return the HTTP response.
 *
 * The controller itself does NO business logic — it's just a thin
 * HTTP adapter. All rules live in AuthService.
 *
 * DESIGN NOTE:
 *   Both endpoints return 200 OK. We could use 201 Created for register
 *   but 200 is more conventional when the response includes an auth token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     *
     * Creates a new user account and returns a JWT so the user
     * is immediately authenticated (REQ-USR-004 AC4).
     *
     * @Valid triggers Jakarta Bean Validation on RegisterRequestDTO.
     * If validation fails, GlobalExceptionHandler returns 400
     * before this method is even called.
     *
     * @param request the registration data (fullName, email, phoneNumber, password)
     * @return 200 OK with AuthResponseDTO (token, userId, fullName)
     *         409 Conflict if email or phone already exists
     *         400 Bad Request if validation fails
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/login
     *
     * Authenticates a user by email or phone + password.
     * Returns a JWT on success (REQ-USR-005 AC1, AC2).
     *
     * @param request the login data (identifier = email or phone, password)
     * @return 200 OK with AuthResponseDTO (token, userId, fullName)
     *         401 Unauthorized if credentials are invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
