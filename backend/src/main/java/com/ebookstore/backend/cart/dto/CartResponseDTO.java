package com.ebookstore.backend.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * CartResponseDTO — full cart response for GET /api/cart and POST /api/cart/merge.
 *
 * REQ-CRT-002, TASK-CRT-003
 *
 * items          — all line items currently in the cart
 * subtotal       — sum of all itemTotals
 * deliveryCharge — always ₹40 (REQ-CHK-002, Decision OQ-001)
 * grandTotal     — subtotal + deliveryCharge
 */
public record CartResponseDTO(
        List<CartItemDTO> items,
        BigDecimal subtotal,
        BigDecimal deliveryCharge,
        BigDecimal grandTotal
) {}
