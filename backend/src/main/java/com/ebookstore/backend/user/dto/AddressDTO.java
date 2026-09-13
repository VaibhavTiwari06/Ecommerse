package com.ebookstore.backend.user.dto;

/**
 * AddressDTO — represents a saved delivery address in API responses.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-USR-006, REQ-CHK-001)
 * =============================================================
 * Returned by:
 *   GET  /api/user/addresses      — list all user's addresses
 *   POST /api/user/addresses      — newly created address
 *   GET  /api/checkout/summary    — addresses available for checkout
 */
public record AddressDTO(
        Long id,
        String label,
        String street,
        String city,
        String state,
        String pincode,
        boolean isDefault
) {}
