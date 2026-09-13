package com.ebookstore.backend.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * OrderDetailDTO — full order detail returned by GET /api/orders/{id}
 * and also embedded in the payment confirmation response (M7).
 *
 * REQ-ORD-001 AC3, API design §8
 *
 * @param id                     order primary key
 * @param createdAt              when order was placed
 * @param status                 current lifecycle status
 * @param items                  line items with price/title snapshots
 * @param subtotal               sum of all item totals (₹)
 * @param deliveryCharge         always ₹40
 * @param giftPointsRedeemed     points used at checkout (0 if none)
 * @param giftPointDiscount      monetary value of redeemed points (₹)
 * @param grandTotal             final amount charged (₹)
 * @param tentativeDeliveryDate  estimated delivery date
 * @param deliveryAddress        snapshot of the delivery address fields
 */
public record OrderDetailDTO(
        Long id,
        Instant createdAt,
        String status,
        List<OrderItemDTO> items,
        BigDecimal subtotal,
        BigDecimal deliveryCharge,
        int giftPointsRedeemed,
        BigDecimal giftPointDiscount,
        BigDecimal grandTotal,
        LocalDate tentativeDeliveryDate,
        AddressSnapshotDTO deliveryAddress
) {
    /**
     * Inline snapshot of address fields — not a FK reference.
     * Means the response is correct even if the address row is later
     * edited or deleted by the user.
     */
    public record AddressSnapshotDTO(
            String street,
            String city,
            String state,
            String pincode
    ) {}
}
