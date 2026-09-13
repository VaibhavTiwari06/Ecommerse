package com.ebookstore.backend.exception;

/**
 * OrderCancellationException — thrown when an order cannot be cancelled.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-ORD-004, design §3.5)
 * =============================================================
 * Per the approved spec (REQ-ORD-004), an order can only be
 * cancelled if its status is CONFIRMED or PROCESSING.
 *
 * If the user tries to cancel an order that is already
 * OUT_FOR_DELIVERY, DELIVERED, or CANCELLED, we throw this.
 *
 * The GlobalExceptionHandler maps this → HTTP 409 Conflict.
 *
 * Usage example:
 *   throw new OrderCancellationException(
 *       "Cannot cancel order with status: DELIVERED");
 */
public class OrderCancellationException extends RuntimeException {

    /**
     * @param message describes why the cancellation was rejected,
     *                e.g. "Cannot cancel an order that is already delivered".
     *                This message IS sent to the client.
     */
    public OrderCancellationException(String message) {
        super(message);
    }
}
