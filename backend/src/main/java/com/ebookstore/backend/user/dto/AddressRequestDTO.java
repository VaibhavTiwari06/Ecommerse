package com.ebookstore.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * AddressRequestDTO — request body for POST /api/user/addresses.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-USR-006, REQ-CHK-001)
 * =============================================================
 * Carries the address data submitted by the user.
 * All required fields are validated with Bean Validation.
 *
 * Note: `isDefault` is optional — if true, this address will be
 * marked as the user's default delivery address.
 */
public record AddressRequestDTO(

        /** Optional friendly name, e.g. "Home", "Office". Can be null. */
        String label,

        @NotBlank(message = "Street address is required")
        String street,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        /** Indian PIN code — 6 digits. */
        @NotBlank(message = "Pincode is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
        String pincode,

        /**
         * Whether to set this as the default address.
         * Defaults to false if not provided.
         */
        Boolean isDefault

) {}
