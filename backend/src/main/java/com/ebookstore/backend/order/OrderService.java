package com.ebookstore.backend.order;

import com.ebookstore.backend.book.DeliveryDateCalculator;
import com.ebookstore.backend.cart.Cart;
import com.ebookstore.backend.cart.CartItem;
import com.ebookstore.backend.cart.CartRepository;
import com.ebookstore.backend.exception.OrderCancellationException;
import com.ebookstore.backend.order.dto.*;
import com.ebookstore.backend.user.Address;
import com.ebookstore.backend.user.AddressRepository;
import com.ebookstore.backend.user.User;
import com.ebookstore.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * OrderService — all order business logic.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-ORD-002–005, REQ-ORD-001–004)
 * =============================================================
 *
 * createOrder(CreateOrderDTO)
 *   Called internally by PaymentService (M7) after simulated payment
 *   success. Validates cart, snapshots prices/titles, awards gift
 *   points, clears cart.  REQ-ORD-001
 *
 * listOrders(userId)
 *   GET /api/orders — all orders newest first.  REQ-ORD-002
 *
 * getOrderDetail(userId, orderId)
 *   GET /api/orders/{id} — full detail including items.  REQ-ORD-002
 *
 * buyAgain(userId, orderId)
 *   POST /api/orders/{id}/buy-again — re-adds items to current cart,
 *   skips out-of-stock books silently.  REQ-ORD-003
 *
 * cancelOrder(userId, orderId)
 *   POST /api/orders/{id}/cancel — only CONFIRMED/PROCESSING are
 *   cancellable; redeemed gift points are forfeited (Decision D-010).
 *   REQ-ORD-004
 *
 * GIFT POINTS (Decision D-008):
 *   earn  = floor(grandTotal / 50)
 *   spend = points × 2 monetary discount
 */
@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final BigDecimal DELIVERY_CHARGE    = BigDecimal.valueOf(40);
    private static final BigDecimal GIFT_POINT_VALUE   = BigDecimal.valueOf(2); // 1pt = ₹2
    private static final BigDecimal POINTS_PER_RUPEE   = BigDecimal.valueOf(50); // ₹50 → 1pt

    private static final Set<OrderStatus> CANCELLABLE =
            Set.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

    private final OrderRepository     orderRepository;
    private final AddressRepository   addressRepository;
    private final UserRepository      userRepository;
    private final CartRepository      cartRepository;

    public OrderService(OrderRepository orderRepository,
                        AddressRepository addressRepository,
                        UserRepository userRepository,
                        CartRepository cartRepository) {
        this.orderRepository   = orderRepository;
        this.addressRepository = addressRepository;
        this.userRepository    = userRepository;
        this.cartRepository    = cartRepository;
    }

    // ------------------------------------------------------------------
    // Create Order — TASK-ORD-002, REQ-ORD-001
    // ------------------------------------------------------------------

    /**
     * Creates an order from the user's current cart.
     *
     * Steps:
     *  1. Load and validate cart is not empty (REQ-ORD-001 AC4)
     *  2. Validate address belongs to user
     *  3. Validate gift points balance is sufficient
     *  4. Snapshot items (price + title)
     *  5. Calculate totals
     *  6. Persist Order + OrderItems
     *  7. Deduct redeemed gift points from user balance
     *  8. Award earned gift points: floor(grandTotal / 50)
     *  9. Clear cart (REQ-ORD-001 AC2)
     * 10. Return OrderDetailDTO
     *
     * Called by PaymentService (M7). Not exposed as an HTTP endpoint here.
     *
     * @param dto validated request from PaymentService
     * @return full OrderDetailDTO
     */
    public OrderDetailDTO createOrder(CreateOrderDTO dto) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + dto.userId()));

        // Step 1 — validate cart not empty
        Cart cart = cartRepository.findByUserIdWithItems(dto.userId())
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with an empty cart");
        }

        // Step 2 — validate address belongs to user
        Address address = addressRepository.findById(dto.addressId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Address not found: " + dto.addressId()));
        if (!address.getUser().getId().equals(dto.userId())) {
            throw new AccessDeniedException("Address does not belong to the authenticated user");
        }

        // Step 3 — validate gift points balance
        int pointsToRedeem = dto.giftPointsToRedeem();
        if (pointsToRedeem > user.getGiftPointBalance()) {
            throw new IllegalArgumentException(
                    "Insufficient gift points: have " + user.getGiftPointBalance()
                    + ", requested " + pointsToRedeem);
        }

        // Step 4–5 — build order + snapshot items + calculate totals
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setDeliveryCharge(DELIVERY_CHARGE);
        order.setGiftPointsRedeemed(pointsToRedeem);
        order.setGiftPointDiscount(
                BigDecimal.valueOf(pointsToRedeem).multiply(GIFT_POINT_VALUE));
        order.setTentativeDeliveryDate(DeliveryDateCalculator.tentativeDeliveryDate());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setBook(cartItem.getBook());
            oi.setTitleSnapshot(cartItem.getBook().getTitle());
            oi.setPriceSnapshot(cartItem.getBook().getPrice());
            oi.setQuantity(cartItem.getQuantity());
            orderItems.add(oi);
            subtotal = subtotal.add(
                    cartItem.getBook().getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setSubtotal(subtotal);
        BigDecimal grandTotal = subtotal
                .add(DELIVERY_CHARGE)
                .subtract(order.getGiftPointDiscount());
        order.setGrandTotal(grandTotal);
        order.setItems(orderItems);

        // Step 6 — persist
        Order saved = orderRepository.save(order);
        log.info("Order created: id={} userId={} grandTotal={}", saved.getId(), dto.userId(), grandTotal);

        // Step 7 — deduct redeemed gift points
        if (pointsToRedeem > 0) {
            user.setGiftPointBalance(user.getGiftPointBalance() - pointsToRedeem);
        }

        // Step 8 — award earned gift points: floor(grandTotal / 50)
        int earned = grandTotal.divideToIntegralValue(POINTS_PER_RUPEE).intValue();
        if (earned > 0) {
            user.setGiftPointBalance(user.getGiftPointBalance() + earned);
            log.debug("Gift points awarded: userId={} earned={} newBalance={}",
                    dto.userId(), earned, user.getGiftPointBalance());
        }
        userRepository.save(user);

        // Step 9 — clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return toDetailDTO(saved);
    }

    // ------------------------------------------------------------------
    // Order History — TASK-ORD-003, REQ-ORD-002
    // ------------------------------------------------------------------

    /**
     * Returns all orders for a user, newest first.
     * REQ-ORD-002 AC1, AC3
     */
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    /**
     * Returns full detail for a single order.
     * REQ-ORD-002 AC2; also used by M7 payment confirmation.
     */
    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderDetail(Long userId, Long orderId) {
        Order order = loadOrderWithOwnerCheck(userId, orderId);
        return toDetailDTO(order);
    }

    // ------------------------------------------------------------------
    // Buy Again — TASK-ORD-004, REQ-ORD-003
    // ------------------------------------------------------------------

    /**
     * Re-adds all items from a past order to the user's current cart.
     * Out-of-stock items are silently skipped and listed in skippedItems.
     * REQ-ORD-003 AC1, AC2
     */
    public BuyAgainResponseDTO buyAgain(Long userId, Long orderId) {
        Order order = loadOrderWithOwnerCheck(userId, orderId);

        List<BuyAgainResponseDTO.BuyAgainItemDTO> added   = new ArrayList<>();
        List<BuyAgainResponseDTO.BuyAgainItemDTO> skipped = new ArrayList<>();

        // Get or create the user's cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(userRepository.getReferenceById(userId));
                    return cartRepository.save(newCart);
                });

        for (OrderItem oi : order.getItems()) {
            if (oi.getBook().getStockQuantity() <= 0) {
                // REQ-ORD-003 AC2 — skip out-of-stock silently, report in response
                skipped.add(new BuyAgainResponseDTO.BuyAgainItemDTO(
                        oi.getBook().getId(),
                        oi.getTitleSnapshot(),
                        oi.getQuantity(),
                        "OUT_OF_STOCK"));
                log.debug("Buy again: skipping out-of-stock book id={}", oi.getBook().getId());
            } else {
                // Add to cart — CartItem upsert logic inline (same as CartService.addItem)
                cart.getItems().stream()
                        .filter(ci -> ci.getBook().getId().equals(oi.getBook().getId()))
                        .findFirst()
                        .ifPresentOrElse(
                                existing -> existing.setQuantity(
                                        existing.getQuantity() + oi.getQuantity()),
                                () -> {
                                    com.ebookstore.backend.cart.CartItem newItem =
                                            new com.ebookstore.backend.cart.CartItem();
                                    newItem.setCart(cart);
                                    newItem.setBook(oi.getBook());
                                    newItem.setQuantity(oi.getQuantity());
                                    cart.getItems().add(newItem);
                                });
                added.add(new BuyAgainResponseDTO.BuyAgainItemDTO(
                        oi.getBook().getId(),
                        oi.getTitleSnapshot(),
                        oi.getQuantity(),
                        null));
            }
        }

        cartRepository.save(cart);
        log.debug("Buy again: userId={} orderId={} added={} skipped={}",
                userId, orderId, added.size(), skipped.size());
        return new BuyAgainResponseDTO(added, skipped);
    }

    // ------------------------------------------------------------------
    // Cancel Order — TASK-ORD-005, REQ-ORD-004
    // ------------------------------------------------------------------

    /**
     * Cancels an order if it is in a cancellable status.
     *
     * Cancellable: CONFIRMED, PROCESSING
     * Non-cancellable → 409 Conflict (OrderCancellationException)
     *
     * Decision D-010: redeemed gift points are NOT restored on cancellation.
     *
     * REQ-ORD-004 AC1, AC2, AC3, AC4
     */
    public CancelOrderResponseDTO cancelOrder(Long userId, Long orderId) {
        Order order = loadOrderWithOwnerCheck(userId, orderId);

        if (!CANCELLABLE.contains(order.getStatus())) {
            throw new OrderCancellationException(
                    "Cannot cancel an order with status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled: id={} userId={}", orderId, userId);

        return new CancelOrderResponseDTO(
                orderId,
                "CANCELLED",
                "Order cancelled successfully. Redeemed gift points are forfeited.");
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Loads an order with items, verifies ownership.
     * Throws 404 if not found, 403 if not the owner.
     */
    private Order loadOrderWithOwnerCheck(Long userId, Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Order does not belong to the authenticated user");
        }
        return order;
    }

    private OrderSummaryDTO toSummaryDTO(Order order) {
        return new OrderSummaryDTO(
                order.getId(),
                order.getCreatedAt(),
                order.getStatus().name(),
                order.getItems().size(),
                order.getGrandTotal()
        );
    }

    private OrderDetailDTO toDetailDTO(Order order) {
        List<OrderItemDTO> items = order.getItems().stream()
                .map(oi -> new OrderItemDTO(
                        oi.getBook().getId(),
                        oi.getTitleSnapshot(),
                        oi.getPriceSnapshot(),
                        oi.getQuantity(),
                        oi.getPriceSnapshot()
                                .multiply(BigDecimal.valueOf(oi.getQuantity()))))
                .toList();

        com.ebookstore.backend.user.Address addr = order.getAddress();
        OrderDetailDTO.AddressSnapshotDTO addressDTO = new OrderDetailDTO.AddressSnapshotDTO(
                addr.getStreet(), addr.getCity(), addr.getState(), addr.getPincode());

        return new OrderDetailDTO(
                order.getId(),
                order.getCreatedAt(),
                order.getStatus().name(),
                items,
                order.getSubtotal(),
                order.getDeliveryCharge(),
                order.getGiftPointsRedeemed(),
                order.getGiftPointDiscount(),
                order.getGrandTotal(),
                order.getTentativeDeliveryDate(),
                addressDTO
        );
    }
}
