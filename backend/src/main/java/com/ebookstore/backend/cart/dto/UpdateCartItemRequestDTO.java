package com.ebookstore.backend.cart.dto;

import jakarta.validation.constraints.Min;

/**
 * UpdateCartItemRequestDTO — request body for PUT /api/cart/items/{itemId}.
 *
 * REQ-CRT-003 AC1, TASK-CRT-002
 *
 * quantity — new quantity (min 1; use DELETE to remove entirely)
 */
public record UpdateCartItemRequestDTO(

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}
