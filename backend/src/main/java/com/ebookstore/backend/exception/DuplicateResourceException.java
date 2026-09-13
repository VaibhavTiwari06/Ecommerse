package com.ebookstore.backend.exception;

/**
 * DuplicateResourceException — thrown when a unique constraint would be violated.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-USR-004, design §3.5)
 * =============================================================
 * When a user tries to register with an email or phone number
 * that already exists in the database, we throw this exception
 * instead of letting the database constraint bubble up as a
 * raw 500 error.
 *
 * The GlobalExceptionHandler maps this → HTTP 409 Conflict with
 * a human-readable message like "Email already in use".
 *
 * Usage examples:
 *   throw new DuplicateResourceException("Email already in use");
 *   throw new DuplicateResourceException("Phone number already registered");
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * @param message a human-readable description of what is duplicated,
     *                e.g. "Email already in use". This message IS sent
     *                to the client — keep it safe (no internals).
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
