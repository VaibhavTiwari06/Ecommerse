package com.ebookstore.backend.payment.dto;

import com.ebookstore.backend.order.dto.OrderItemDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PaymentResponseDTO — purchase confirmation returned by POST /api/payment/initiate.
 *
 * REQ-PAY-004 AC1, REQ-PAY-005 AC2, API design §9
 *
 * @param orderId              the created order's ID
 * @param paymentReference     simulated payment reference ("PAY-" + UUID)
 * @param paymentMethod        the method used (CREDIT_CARD / DEBIT_CARD)
 * @param items                items purchased (with title/price snapshots)
 * @param subtotal             sum of item totals before discount / delivery
 * @param deliveryCharge       always ₹40
 * @param giftPointsRedeemed   points spent (0 if none)
 * @param giftPointDiscount    monetary discount from points (₹)
 * @param grandTotal           final amount charged (₹)
 * @param giftPointsEarned     points earned on this order = floor(grandTotal / 50)
 * @param tentativeDeliveryDate estimated delivery date
 * @param message              human-readable confirmation message
 */
public record PaymentResponseDTO(
        Long            orderId,
        String          paymentReference,
        String          paymentMethod,
        List<OrderItemDTO> items,
        BigDecimal      subtotal,
        BigDecimal      deliveryCharge,
        int             giftPointsRedeemed,
        BigDecimal      giftPointDiscount,
        BigDecimal      grandTotal,
        int             giftPointsEarned,
        LocalDate       tentativeDeliveryDate,
        String          message
) {}
