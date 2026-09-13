package com.ebookstore.backend.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * MergeCartRequestDTO — request body for POST /api/cart/merge.
 *
 * REQ-CRT-005, REQ-USR-003, TASK-CRT-005
 *
 * Sent by the frontend immediately after login to merge the guest
 * localStorage cart into the authenticated user's server cart.
 *
 * items — list of {bookId, quantity} from localStorage.
 *         Empty list is valid — results in a no-op merge.
 */
public record MergeCartRequestDTO(

        @NotNull(message = "Items list is required")
        @Valid
        List<MergeItem> items
) {
    /**
     * One item from the guest localStorage cart.
     */
    public record MergeItem(

            @NotNull(message = "Book ID is required")
            Long bookId,

            @Min(value = 1, message = "Quantity must be at least 1")
            int quantity
    ) {}
}
