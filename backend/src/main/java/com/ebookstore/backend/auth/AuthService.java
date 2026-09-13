package com.ebookstore.backend.auth;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.LoginRequestDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import com.ebookstore.backend.exception.DuplicateResourceException;
import com.ebookstore.backend.user.User;
import com.ebookstore.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService — handles user registration and login business logic.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-002, TASK-AUTH-003, REQ-USR-004, REQ-USR-005)
 * =============================================================
 * AuthService is the single place responsible for:
 *
 *  REGISTRATION:
 *    1. Check for duplicate email → 409 if found
 *    2. Check for duplicate phone → 409 if found
 *    3. Hash the password with BCrypt
 *    4. Save the new User to the DB
 *    5. Issue and return a JWT (user is immediately logged in)
 *
 *  LOGIN:
 *    1. Find user by email OR phone number (CR-001)
 *    2. Verify password with BCrypt.matches()
 *    3. Issue and return a JWT
 *
 * SECURITY RULES ENFORCED HERE:
 *   - Passwords are NEVER stored plaintext (BCrypt hash only).
 *   - Login failure always throws the SAME exception regardless of
 *     whether the user wasn't found or the password was wrong.
 *     This prevents user enumeration attacks (REQ-USR-005 AC3).
 *
 * @Transactional on register() ensures that if the save fails
 * (e.g. constraint violation we didn't catch), the whole operation
 * rolls back cleanly.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /**
     * Registers a new user account.
     *
     * Steps:
     *   1. Check email uniqueness — throw DuplicateResourceException if taken.
     *   2. Check phone uniqueness — throw DuplicateResourceException if taken.
     *   3. Hash the password with BCrypt (strength 12, configured in SecurityConfig).
     *   4. Persist the new User.
     *   5. Generate and return a JWT so the user is immediately logged in.
     *
     * @param request validated registration data from the controller
     * @return AuthResponseDTO containing the JWT token and user info
     * @throws DuplicateResourceException if email or phone already exists (409)
     */
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {

        // --- Duplicate email check (REQ-USR-004 AC1) ---
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email address is already in use");
        }

        // --- Duplicate phone check (REQ-USR-004 AC2) ---
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException("Phone number is already registered");
        }

        // --- Build and persist the User entity ---
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());

        // Hash the password — NEVER store plaintext (REQ-USR-004 AC3)
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        // gift_point_balance starts at 0 (default on the entity)

        User saved = userRepository.save(user);
        log.info("New user registered: id={}, email={}", saved.getId(), saved.getEmail());

        // --- Issue JWT — user is immediately logged in (REQ-USR-004 AC4) ---
        String token = jwtService.generateToken(saved);
        return new AuthResponseDTO(token, saved.getId(), saved.getFullName());
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    /**
     * Authenticates a user and issues a JWT.
     *
     * The identifier can be the user's email OR phone number (CR-001).
     * The same generic error is thrown whether the user is not found
     * OR the password is wrong — this prevents user enumeration.
     *
     * @param request validated login data (identifier + password)
     * @return AuthResponseDTO containing the JWT token and user info
     * @throws BadCredentialsException if credentials are invalid (maps to 401)
     */
    public AuthResponseDTO login(LoginRequestDTO request) {

        // --- Find user by email OR phone (CR-001, REQ-USR-005) ---
        //
        // We pass the identifier twice — the JPQL query uses OR:
        //   WHERE email = :email OR phone_number = :phone
        // So passing the same value for both parameters effectively means:
        //   WHERE email = :id OR phone_number = :id
        User user = userRepository
                .findByEmailOrPhoneNumber(request.identifier(), request.identifier())
                .orElseThrow(() ->
                    // SECURITY: Generic message — don't say "user not found"
                    // which would confirm the identifier exists or not
                    new BadCredentialsException("Invalid credentials")
                );

        // --- Verify password with BCrypt (REQ-USR-005 AC6) ---
        //
        // passwordEncoder.matches(rawPassword, storedHash):
        //   - extracts the salt from storedHash
        //   - hashes rawPassword with that salt
        //   - compares the result to storedHash
        // This is timing-safe and correct for BCrypt.
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // SECURITY: Same exception as "user not found" — no enumeration
            throw new BadCredentialsException("Invalid credentials");
        }

        log.debug("User login successful: id={}", user.getId());

        // --- Issue JWT (REQ-USR-005 AC1, AC2) ---
        String token = jwtService.generateToken(user);
        return new AuthResponseDTO(token, user.getId(), user.getFullName());
    }
}
