package com.ebookstore.backend.order.dto;

import java.util.List;

/**
 * BuyAgainResponseDTO — response for POST /api/orders/{orderId}/buy-again.
 *
 * REQ-ORD-003 AC1, AC2; API design §8
 *
 * @param addedItems   books successfully added to the current cart
 * @param skippedItems books that could not be added (e.g. out of stock)
 */
public record BuyAgainResponseDTO(
        List<BuyAgainItemDTO> addedItems,
        List<BuyAgainItemDTO> skippedItems
) {
    /**
     * One entry per book line from the original order.
     *
     * @param bookId   FK to books table
     * @param title    title snapshot from the original order
     * @param quantity quantity from the original order
     * @param reason   null for addedItems; "OUT_OF_STOCK" for skippedItems
     */
    public record BuyAgainItemDTO(
            Long bookId,
            String title,
            int quantity,
            String reason
    ) {}
}
