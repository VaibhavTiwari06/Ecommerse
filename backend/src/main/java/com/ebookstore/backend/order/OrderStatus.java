package com.ebookstore.backend.order;

/**
 * OrderStatus — valid lifecycle states for an Order.
 *
 * =============================================================
 * WHY THIS ENUM EXISTS (TASK-ORD-001, REQ-ORD-004, design §2.7)
 * =============================================================
 * The approved spec defines a fixed status lifecycle:
 *   CONFIRMED → PROCESSING → OUT_FOR_DELIVERY → DELIVERED
 *
 * Cancellation is only allowed from CONFIRMED or PROCESSING.
 * Attempting to cancel from any other status → 409 Conflict.
 *
 * Stored in the DB as a VARCHAR(30) string (not ordinal) so that
 * adding new statuses in future never corrupts existing records.
 *
 * Decision D-010: redeemed gift points are forfeited on cancellation —
 * that rule lives in OrderService, not here.
 */
public enum OrderStatus {

    /**
     * Order placed and payment confirmed.
     * Cancellable: YES
     */
    CONFIRMED,

    /**
     * Order is being packed/processed at the warehouse.
     * Cancellable: YES
     */
    PROCESSING,

    /**
     * Order is with the delivery partner.
     * Cancellable: NO → 409 if attempted
     */
    OUT_FOR_DELIVERY,

    /**
     * Order delivered to customer.
     * Cancellable: NO → 409 if attempted
     */
    DELIVERED,

    /**
     * Order was cancelled by the customer.
     * Cancellable: NO (already cancelled) → 409 if attempted again
     */
    CANCELLED
}
