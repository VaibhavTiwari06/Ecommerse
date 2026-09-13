package com.ebookstore.backend.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * AddToCartRequestDTO — request body for POST /api/cart/items.
 *
 * REQ-CRT-001, TASK-CRT-002
 *
 * bookId   — the book to add (required)
 * quantity — how many copies to add (min 1)
 */
public record AddToCartRequestDTO(

        @NotNull(message = "Book ID is required")
        Long bookId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}
