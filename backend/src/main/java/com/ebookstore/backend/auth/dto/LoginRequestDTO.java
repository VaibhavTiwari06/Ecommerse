package com.ebookstore.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequestDTO — the request body for POST /api/auth/login.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-003, REQ-USR-005, CR-001)
 * =============================================================
 * Per CR-001, a user can log in using EITHER their email address
 * OR their 10-digit phone number. The single `identifier` field
 * accepts both formats — AuthService resolves which one it is
 * by querying: WHERE email = :id OR phone_number = :id.
 *
 * We intentionally use "identifier" rather than "email" or "phone"
 * to avoid giving the caller any hint about what format is expected
 * (reduces attack surface for enumeration).
 */
public record LoginRequestDTO(

        /**
         * Either the user's email address or their 10-digit phone number.
         * AuthService looks up the user by both fields using OR logic.
         */
        @NotBlank(message = "Identifier (email or phone number) is required")
        String identifier,

        /**
         * The user's password in plaintext.
         * AuthService uses BCrypt.matches() to verify — never stores raw.
         */
        @NotBlank(message = "Password is required")
        String password

) {}
