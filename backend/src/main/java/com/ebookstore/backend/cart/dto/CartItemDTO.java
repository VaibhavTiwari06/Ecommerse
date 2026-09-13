package com.ebookstore.backend.cart.dto;

import java.math.BigDecimal;

/**
 * CartItemDTO — a single line item in the cart response.
 *
 * REQ-CRT-002, TASK-CRT-003
 *
 * id           — CartItem PK (used for PUT/DELETE endpoints)
 * bookId       — Book PK (used by frontend to link to book detail page)
 * title        — book title (display)
 * coverImageUrl — book cover (display)
 * price        — unit price at the time of view (may differ from purchase snapshot)
 * quantity     — current quantity in cart
 * itemTotal    — price × quantity
 */
public record CartItemDTO(
        Long id,
        Long bookId,
        String title,
        String coverImageUrl,
        BigDecimal price,
        int quantity,
        BigDecimal itemTotal
) {}
