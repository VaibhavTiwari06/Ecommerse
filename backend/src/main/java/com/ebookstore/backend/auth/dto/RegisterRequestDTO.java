package com.ebookstore.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequestDTO — the request body for POST /api/auth/register.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-002, REQ-USR-004)
 * =============================================================
 * This DTO carries the data a new user submits during registration.
 * Jakarta Bean Validation annotations on each field mean Spring will
 * automatically validate the request before it reaches AuthService.
 * If any constraint fails, GlobalExceptionHandler returns 400.
 *
 * VALIDATION RULES (REQ-USR-004):
 *   fullName    — must not be blank
 *   email       — must be a valid email format
 *   phoneNumber — exactly 10 digits (Indian mobile, CR-001, AC6)
 *   password    — minimum 8 characters (AC5)
 *
 * We use a Java record — immutable, compact, auto-generates
 * constructor/getters/equals/hashCode. Perfect for DTOs.
 */
public record RegisterRequestDTO(

        /**
         * The user's full name for display purposes.
         * @NotBlank rejects null, empty string, and whitespace-only.
         */
        @NotBlank(message = "Full name is required")
        String fullName,

        /**
         * Email address — used as a login identifier.
         * @Email validates format (must contain @ and a domain).
         */
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        /**
         * 10-digit Indian mobile number — second login identifier (CR-001).
         * @Pattern enforces exactly 10 numeric digits.
         * Leading zeros are preserved since it's stored as a string.
         */
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9]{10}$",
                message = "Phone number must be exactly 10 digits")
        String phoneNumber,

        /**
         * The user's chosen password.
         * @Size enforces minimum 8 characters.
         * This raw value is BCrypt-hashed by AuthService before storage.
         */
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

) {}
