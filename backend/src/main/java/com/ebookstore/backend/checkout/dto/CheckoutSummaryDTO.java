package com.ebookstore.backend.checkout.dto;

import com.ebookstore.backend.cart.dto.CartItemDTO;
import com.ebookstore.backend.user.dto.AddressDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CheckoutSummaryDTO — full checkout page payload.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CHK-001, REQ-CHK-001–003, design §2.8)
 * =============================================================
 * The frontend checkout page needs everything in one response:
 * cart items, totals, the user's saved addresses, gift point
 * balance, and the tentative delivery date. This avoids multiple
 * round-trips from the UI.
 *
 * FIELDS:
 *   items                — cart line items (re-uses CartItemDTO)
 *   subtotal             — sum of item totals before delivery / discount
 *   deliveryCharge       — always ₹40 (REQ-CHK-002, Decision OQ-001)
 *   grandTotal           — subtotal + deliveryCharge (before gift points)
 *   giftPointBalance     — user's current available points
 *   maxGiftPointDiscount — monetary value of full balance: balance × 2
 *                          (1pt = ₹2, Decision D-008), capped at grandTotal
 *   tentativeDeliveryDate — today + 5 business days (REQ-CHK-003)
 *   savedAddresses       — all of the user's saved delivery addresses
 */
public record CheckoutSummaryDTO(
        List<CartItemDTO>  items,
        BigDecimal         subtotal,
        BigDecimal         deliveryCharge,
        BigDecimal         grandTotal,
        int                giftPointBalance,
        BigDecimal         maxGiftPointDiscount,
        LocalDate          tentativeDeliveryDate,
        List<AddressDTO>   savedAddresses
) {}
