package com.ebookstore.backend.order;

import com.ebookstore.backend.auth.JwtService;
import com.ebookstore.backend.order.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OrderController — HTTP layer for all /api/orders/** endpoints.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-ORD-003–005, REQ-ORD-002–004)
 * =============================================================
 *
 * GET  /api/orders                        — list orders (REQ-ORD-002)
 * GET  /api/orders/{orderId}              — order detail (REQ-ORD-002 AC2)
 * POST /api/orders/{orderId}/buy-again    — re-add items to cart (REQ-ORD-003)
 * POST /api/orders/{orderId}/cancel       — cancel order (REQ-ORD-004)
 *
 * NOTE: Order CREATION has no dedicated HTTP endpoint in M5.
 * createOrder() is called internally by PaymentService (M7) within
 * the same transaction as payment processing (REQ-ORD-001, REQ-PAY-003).
 *
 * ALL endpoints require a valid JWT (enforced by SecurityConfig).
 * The authenticated user's ID is extracted from the JWT via JwtService.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtService   jwtService;

    public OrderController(OrderService orderService, JwtService jwtService) {
        this.orderService = orderService;
        this.jwtService   = jwtService;
    }

    // ------------------------------------------------------------------
    // GET /api/orders — list orders (TASK-ORD-003, REQ-ORD-002)
    // ------------------------------------------------------------------

    /**
     * Returns all orders for the authenticated user, newest first.
     * REQ-ORD-002 AC1: returns list (empty list for new users, not 404)
     * REQ-ORD-002 AC3: empty list when no orders
     */
    @GetMapping
    public ResponseEntity<List<OrderSummaryDTO>> listOrders(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(orderService.listOrders(userId));
    }

    // ------------------------------------------------------------------
    // GET /api/orders/{orderId} — order detail (REQ-ORD-002 AC2)
    // ------------------------------------------------------------------

    /**
     * Returns full detail for a single order including items, address,
     * and all financial totals.
     * Returns 403 if the order does not belong to the authenticated user.
     * Returns 404 if the order does not exist.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDTO> getOrderDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(orderService.getOrderDetail(userId, orderId));
    }

    // ------------------------------------------------------------------
    // POST /api/orders/{orderId}/buy-again (TASK-ORD-004, REQ-ORD-003)
    // ------------------------------------------------------------------

    /**
     * Re-adds all items from a past order to the current cart.
     * Out-of-stock items are skipped and reported in skippedItems.
     * REQ-ORD-003 AC1, AC2, AC3
     */
    @PostMapping("/{orderId}/buy-again")
    public ResponseEntity<BuyAgainResponseDTO> buyAgain(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(orderService.buyAgain(userId, orderId));
    }

    // ------------------------------------------------------------------
    // POST /api/orders/{orderId}/cancel (TASK-ORD-005, REQ-ORD-004)
    // ------------------------------------------------------------------

    /**
     * Cancels an order if it is in a cancellable status (CONFIRMED or PROCESSING).
     * Returns 409 Conflict if status is OUT_FOR_DELIVERY, DELIVERED, or CANCELLED.
     * Returns 403 if the order does not belong to the authenticated user.
     * Redeemed gift points are NOT restored (Decision D-010).
     * REQ-ORD-004 AC1, AC2, AC3, AC4
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<CancelOrderResponseDTO> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
    }

    // ------------------------------------------------------------------
    // Private helper
    // ------------------------------------------------------------------

    /** Extracts the authenticated user's ID from the Bearer token. */
    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7); // strip "Bearer "
        return jwtService.extractUserId(token);
    }
}
