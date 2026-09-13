package com.ebookstore.backend.order.dto;

import java.math.BigDecimal;

/**
 * OrderItemDTO — one line item inside an OrderDetailDTO.
 *
 * REQ-ORD-001 AC3: order includes all cart line items with
 * price snapshots (not live book prices).
 *
 * @param bookId         FK to books table (used for Buy Again lookup)
 * @param titleSnapshot  book title at time of purchase
 * @param priceSnapshot  price per unit (₹) at time of purchase
 * @param quantity       number of copies
 * @param itemTotal      priceSnapshot × quantity
 */
public record OrderItemDTO(
        Long bookId,
        String titleSnapshot,
        BigDecimal priceSnapshot,
        int quantity,
        BigDecimal itemTotal
) {}
