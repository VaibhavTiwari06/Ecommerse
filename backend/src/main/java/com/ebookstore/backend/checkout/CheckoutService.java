package com.ebookstore.backend.checkout;

import com.ebookstore.backend.book.DeliveryDateCalculator;
import com.ebookstore.backend.cart.Cart;
import com.ebookstore.backend.cart.CartRepository;
import com.ebookstore.backend.cart.dto.CartItemDTO;
import com.ebookstore.backend.checkout.dto.CheckoutSummaryDTO;
import com.ebookstore.backend.user.UserRepository;
import com.ebookstore.backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * CheckoutService — builds the checkout summary for GET /api/checkout/summary.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CHK-001, REQ-CHK-001–003, design §2.8)
 * =============================================================
 * Assembles everything the checkout page needs in a single call:
 *
 *   1. Cart items (from CartRepository — same data as GET /api/cart)
 *   2. Subtotal, deliveryCharge (₹40), grandTotal
 *   3. User's gift point balance
 *   4. maxGiftPointDiscount = min(balance × 2, grandTotal)
 *      — the most the user can knock off the order (Decision D-008)
 *   5. Tentative delivery date = today + 5 business days (REQ-CHK-003)
 *   6. Saved addresses (all addresses for this user — REQ-CHK-001)
 *
 * Empty cart: returns items=[], subtotal=0, grandTotal=40 (just delivery).
 * No addresses: savedAddresses=[]. Neither case is an error.
 */
@Service
@Transactional(readOnly = true)
public class CheckoutService {

    private static final BigDecimal DELIVERY_CHARGE  = BigDecimal.valueOf(40);
    private static final BigDecimal GIFT_POINT_VALUE = BigDecimal.valueOf(2); // 1pt = ₹2

    private final CartRepository  cartRepository;
    private final UserRepository  userRepository;
    private final UserService     userService;

    public CheckoutService(CartRepository cartRepository,
                           UserRepository userRepository,
                           UserService userService) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.userService    = userService;
    }

    /**
     * Builds the full checkout summary for the authenticated user.
     *
     * REQ-CHK-001 AC2: savedAddresses includes all user's saved addresses.
     * REQ-CHK-002 AC1: deliveryCharge is always ₹40.
     * REQ-CHK-003 AC1, AC2: tentativeDeliveryDate = today + 5 business days.
     *
     * @param userId authenticated user's ID
     * @return CheckoutSummaryDTO
     */
    public CheckoutSummaryDTO getSummary(Long userId) {

        // --- Cart items ---
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);

        List<CartItemDTO> items;
        BigDecimal subtotal;

        if (cart == null || cart.getItems().isEmpty()) {
            items    = List.of();
            subtotal = BigDecimal.ZERO;
        } else {
            items = cart.getItems().stream()
                    .map(ci -> new CartItemDTO(
                            ci.getId(),
                            ci.getBook().getId(),
                            ci.getBook().getTitle(),
                            ci.getBook().getCoverImageUrl(),
                            ci.getBook().getPrice(),
                            ci.getQuantity(),
                            ci.getBook().getPrice()
                                    .multiply(BigDecimal.valueOf(ci.getQuantity()))))
                    .toList();
            subtotal = items.stream()
                    .map(CartItemDTO::itemTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal grandTotal = subtotal.add(DELIVERY_CHARGE);

        // --- Gift points ---
        int balance = userRepository.findById(userId)
                .map(u -> u.getGiftPointBalance())
                .orElse(0);

        // Max discount = balance × 2, but never more than the grandTotal
        // (can't get money back — REQ-GFT-002, D-008)
        BigDecimal maxDiscount = BigDecimal.valueOf(balance)
                .multiply(GIFT_POINT_VALUE)
                .min(grandTotal);

        // --- Addresses ---
        var addresses = userService.getAddresses(userId);

        // --- Delivery date ---
        var deliveryDate = DeliveryDateCalculator.tentativeDeliveryDate();

        return new CheckoutSummaryDTO(
                items,
                subtotal,
                DELIVERY_CHARGE,
                grandTotal,
                balance,
                maxDiscount,
                deliveryDate,
                addresses
        );
    }
}
