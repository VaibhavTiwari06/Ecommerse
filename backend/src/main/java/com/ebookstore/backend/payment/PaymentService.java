package com.ebookstore.backend.payment;

import com.ebookstore.backend.order.OrderService;
import com.ebookstore.backend.order.dto.CreateOrderDTO;
import com.ebookstore.backend.order.dto.OrderDetailDTO;
import com.ebookstore.backend.payment.dto.PaymentRequestDTO;
import com.ebookstore.backend.payment.dto.PaymentResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PaymentService — orchestrates the simulated payment flow.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-PAY-001, REQ-PAY-001–005, design §2.9)
 * =============================================================
 *
 * Transaction steps (design §2.9):
 *   1. Validate paymentMethod is CREDIT_CARD or DEBIT_CARD
 *      (Jackson enum deserialisation handles this — invalid value → 400
 *      before this method is even called; this step is a safety guard).
 *   2. Delegate to OrderService.createOrder() which:
 *        - Validates cart not empty
 *        - Validates address belongs to user
 *        - Validates giftPointsToRedeem ≤ balance
 *        - Snapshots prices, builds Order + OrderItems
 *        - Deducts redeemed gift points
 *        - Awards earned gift points (floor(grandTotal / 50))
 *        - Clears the cart
 *   3. Generate a simulated payment reference (UUID).
 *   4. Build and return PaymentResponseDTO.
 *
 * NOTE: No Payment entity is persisted in M7 — the design mentions a
 * payments table (V9) but the spec does not require it for the frontend
 * confirmation flow. The payment reference UUID is returned for display.
 * A V9 migration + Payment entity can be added in a future change request.
 *
 * SIMULATION: Payment always succeeds for valid inputs (REQ-PAY-001 AC2).
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final BigDecimal POINTS_PER_RUPEE = BigDecimal.valueOf(50);

    private final OrderService orderService;

    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Processes a simulated payment and creates an order.
     *
     * @param userId  authenticated user's ID
     * @param request payment request (addressId, paymentMethod, giftPointsToRedeem)
     * @return full purchase confirmation
     */
    public PaymentResponseDTO initiate(Long userId, PaymentRequestDTO request) {

        // Delegate all business logic (validation, order creation, points, cart clear)
        // to OrderService — single source of truth for order lifecycle (REQ-PAY-003)
        OrderDetailDTO order = orderService.createOrder(
                new CreateOrderDTO(userId, request.addressId(), request.giftPointsToRedeem()));

        // Simulated payment reference — prefixed for readability (REQ-PAY-004 AC1)
        String paymentRef = "PAY-" + UUID.randomUUID();

        // Points earned = floor(grandTotal / 50) — same formula as in OrderService
        int pointsEarned = order.grandTotal()
                .divideToIntegralValue(POINTS_PER_RUPEE)
                .intValue();

        log.info("Payment processed: userId={} orderId={} ref={} grand={}",
                userId, order.id(), paymentRef, order.grandTotal());

        return new PaymentResponseDTO(
                order.id(),
                paymentRef,
                request.paymentMethod().name(),
                order.items(),
                order.subtotal(),
                order.deliveryCharge(),
                order.giftPointsRedeemed(),
                order.giftPointDiscount(),
                order.grandTotal(),
                pointsEarned,
                order.tentativeDeliveryDate(),
                "Purchase successful! Thank you for your order."
        );
    }
}
