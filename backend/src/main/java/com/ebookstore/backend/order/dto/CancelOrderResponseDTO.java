package com.ebookstore.backend.order.dto;

/**
 * CancelOrderResponseDTO — response for POST /api/orders/{orderId}/cancel.
 *
 * REQ-ORD-004 AC1; API design §8
 *
 * @param orderId  the cancelled order's ID
 * @param status   always "CANCELLED"
 * @param message  human-readable confirmation including gift-point forfeiture notice
 */
public record CancelOrderResponseDTO(
        Long orderId,
        String status,
        String message
) {}
