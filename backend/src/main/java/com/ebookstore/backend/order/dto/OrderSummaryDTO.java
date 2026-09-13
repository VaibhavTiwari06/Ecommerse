package com.ebookstore.backend.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OrderSummaryDTO — lightweight order entry for GET /api/orders list.
 *
 * REQ-ORD-002 AC2: each entry includes orderId, date, status,
 * item count, and grand total.
 *
 * @param id          order primary key
 * @param createdAt   when the order was placed
 * @param status      current order status (e.g. "CONFIRMED", "DELIVERED")
 * @param itemCount   total number of distinct book lines in the order
 * @param grandTotal  final amount charged (₹), including delivery
 */
public record OrderSummaryDTO(
        Long id,
        Instant createdAt,
        String status,
        int itemCount,
        BigDecimal grandTotal
) {}
