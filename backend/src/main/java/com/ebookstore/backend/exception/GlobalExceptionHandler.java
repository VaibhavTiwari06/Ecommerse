package com.ebookstore.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — centralised error handling for the entire API.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-NFR-003, REQ-NFR-006, design §3.5)
 * =============================================================
 * Without this class, Spring would return different error shapes
 * depending on which exception was thrown (sometimes HTML, sometimes
 * Spring's default JSON, sometimes raw stack traces).
 *
 * This class ensures:
 *  1. EVERY error response has the same JSON shape:
 *       { "status": 400, "error": "Bad Request",
 *         "message": "...", "timestamp": "..." }
 *
 *  2. NO stack traces, internal class names, or SQL errors are
 *     ever exposed to the client (REQ-NFR-006, security rules).
 *
 *  3. Error details are always logged server-side for debugging.
 *
 * HOW IT WORKS:
 *   @RestControllerAdvice turns this class into an interceptor.
 *   Each @ExceptionHandler method catches one exception type,
 *   logs it if necessary, and returns a structured ResponseEntity.
 *
 * MAPPING TABLE (design §3.5):
 *   MethodArgumentNotValidException → 400 (bean validation failures)
 *   IllegalArgumentException        → 400
 *   InsufficientGiftPointsException → 400
 *   AuthenticationException         → 401 (generic message only)
 *   AccessDeniedException           → 403
 *   EntityNotFoundException         → 404
 *   DuplicateResourceException      → 409
 *   OrderCancellationException      → 409
 *   Exception (catch-all)           → 500 (generic message, full log)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Logger for server-side error recording — REQ-NFR-006
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ----------------------------------------------------------------
    // 400 BAD REQUEST
    // ----------------------------------------------------------------

    /**
     * Handles @Valid / @Validated bean validation failures.
     * Example: missing required field, phone number too short, etc.
     * All field error messages are joined into a single readable string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        // Collect all field-level validation messages, e.g.:
        // "Full name is required, Password must be at least 8 characters"
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handles explicit argument validation we throw in service code.
     * Example: "Cart is empty, cannot place order"
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles unreadable/malformed request bodies — including invalid enum values.
     * Example: paymentMethod = "CASH" → 400 (REQ-PAY-002 AC2)
     * SECURITY: returns a generic message; never leaks internal class names.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex) {
        log.debug("Unreadable message: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Invalid request body: check field types and allowed values");
    }

    /**
     * Handles gift point balance insufficient errors (REQ-GFT-002).
     */
    @ExceptionHandler(InsufficientGiftPointsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientGiftPoints(
            InsufficientGiftPointsException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 401 UNAUTHORIZED
    // ----------------------------------------------------------------

    /**
     * Handles authentication failures — wrong password, bad token, etc.
     *
     * SECURITY NOTE (REQ-USR-005 AC3):
     * We return a GENERIC message. We do NOT say "email not found" vs
     * "wrong password" — that distinction would help attackers enumerate
     * valid email addresses.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex) {
        // Log at debug only — this is expected for wrong passwords
        log.debug("Authentication failure: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    // ----------------------------------------------------------------
    // 403 FORBIDDEN
    // ----------------------------------------------------------------

    /**
     * Handles access denied — user is authenticated but trying to access
     * a resource that belongs to someone else (e.g. another user's order).
     * REQ-ORD-004 AC3, REQ-ORD-003 AC3.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Access denied");
    }

    // ----------------------------------------------------------------
    // 404 NOT FOUND
    // ----------------------------------------------------------------

    /**
     * Handles JPA EntityNotFoundException — thrown when a repository
     * lookup returns nothing (book not found, order not found, etc.).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 409 CONFLICT
    // ----------------------------------------------------------------

    /**
     * Handles duplicate resource errors — duplicate email or phone
     * during registration (REQ-USR-004 AC1, AC2).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles order cancellation state conflicts — e.g. trying to cancel
     * a delivered order (REQ-ORD-004 AC2).
     */
    @ExceptionHandler(OrderCancellationException.class)
    public ResponseEntity<ErrorResponse> handleOrderCancellation(
            OrderCancellationException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ----------------------------------------------------------------
    // 500 INTERNAL SERVER ERROR — catch-all
    // ----------------------------------------------------------------

    /**
     * Catches any exception not handled above.
     *
     * SECURITY RULE (REQ-NFR-006):
     * We log the full stack trace server-side for debugging BUT
     * we return only a generic "An unexpected error occurred" to
     * the client. Never leak SQL errors, class names, or stack
     * traces to the outside world.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Full stack trace logged server-side only
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    // ----------------------------------------------------------------
    // Helper — builds the standard error response body
    // ----------------------------------------------------------------

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(),
                        message, Instant.now().toString()));
    }

    // ----------------------------------------------------------------
    // ErrorResponse — the standard JSON shape for ALL error responses
    // ----------------------------------------------------------------

    /**
     * The consistent error body sent to clients.
     *
     * Example JSON:
     * {
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "Phone number must be exactly 10 digits",
     *   "timestamp": "2026-08-24T10:00:00Z"
     * }
     *
     * We use a Java record — it auto-generates constructor, getters,
     * equals, hashCode, and toString. Jackson serialises it to JSON.
     */
    public record ErrorResponse(
            int status,
            String error,
            String message,
            String timestamp) {
    }
}
