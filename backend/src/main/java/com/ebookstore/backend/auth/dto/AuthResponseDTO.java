package com.ebookstore.backend.auth.dto;

/**
 * AuthResponseDTO — the response body for both register and login.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-002, TASK-AUTH-003, REQ-USR-005)
 * =============================================================
 * After successful authentication (register or login), we return:
 *   - token    : the JWT the frontend must include in every subsequent
 *                request as "Authorization: Bearer <token>"
 *   - userId   : the user's database ID (useful for frontend state)
 *   - fullName : displayed in the UI navbar/profile header
 *
 * SECURITY NOTE:
 *   We do NOT return the email, phone, or password hash here.
 *   The frontend only needs enough to display the user's name
 *   and to authenticate future requests.
 */
public record AuthResponseDTO(

        /** The JWT access token. Valid for 24 hours (app.jwt.expiration-ms). */
        String token,

        /** The user's database ID. Used by the frontend to identify the user. */
        Long userId,

        /** The user's full name for display in the UI. */
        String fullName

) {}
