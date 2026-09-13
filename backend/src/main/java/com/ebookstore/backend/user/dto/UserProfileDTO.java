package com.ebookstore.backend.user.dto;

/**
 * UserProfileDTO — the response body for GET /api/user/profile.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-USR-006)
 * =============================================================
 * We never return the raw User entity from controllers because it
 * contains the passwordHash field. This DTO exposes only the safe,
 * user-facing fields.
 *
 * Fields:
 *   userId          — the user's database ID
 *   fullName        — display name
 *   email           — email address
 *   phoneNumber     — phone number
 *   giftPointBalance — current gift point total
 */
public record UserProfileDTO(
        Long userId,
        String fullName,
        String email,
        String phoneNumber,
        int giftPointBalance
) {}
