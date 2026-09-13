package com.ebookstore.backend.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * CreateOrderDTO — internal request object passed from PaymentService
 * to OrderService.createOrder() (M7).
 *
 * Not exposed as an HTTP request body — the public-facing DTO is
 * PaymentRequestDTO (M7). This DTO carries the validated, resolved
 * values needed to build the Order entity.
 *
 * REQ-ORD-001, TASK-ORD-002
 *
 * @param userId              authenticated user placing the order
 * @param addressId           delivery address (must belong to userId)
 * @param giftPointsToRedeem  points to deduct from user balance (≥ 0)
 */
public record CreateOrderDTO(
        @NotNull Long userId,
        @NotNull Long addressId,
        @Min(0)  int giftPointsToRedeem
) {}
