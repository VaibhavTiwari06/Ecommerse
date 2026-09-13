package com.ebookstore.backend.cart;

import com.ebookstore.backend.cart.dto.*;
import com.ebookstore.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * CartController — handles all /api/cart/** endpoints.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CRT-002, TASK-CRT-003, TASK-CRT-005)
 * =============================================================
 * ALL endpoints require JWT authentication (REQ-CRT-001 AC4).
 * SecurityConfig already enforces this via anyRequest().authenticated()
 * for /api/cart/**.
 *
 * Endpoints:
 *   GET    /api/cart                   — view cart (REQ-CRT-002)
 *   POST   /api/cart/items             — add item (REQ-CRT-001)
 *   PUT    /api/cart/items/{itemId}    — update quantity (REQ-CRT-003 AC1)
 *   DELETE /api/cart/items/{itemId}    — remove item (REQ-CRT-003 AC2)
 *   POST   /api/cart/merge             — merge guest cart on login (REQ-CRT-005)
 *
 * The controller is thin — it validates input, delegates to CartService,
 * and returns the HTTP response. No business logic lives here.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * GET /api/cart
     *
     * Returns the authenticated user's cart with all items, subtotal,
     * ₹40 delivery charge, and grand total.
     * Returns an empty cart (not 404) if the user has no cart yet.
     *
     * REQ-CRT-002
     */
    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(cartService.getCart(currentUser.getId()));
    }

    /**
     * POST /api/cart/items
     *
     * Adds a book to the cart. If already present, increments quantity.
     * Returns 400 if the book is out of stock.
     * Returns 404 if the book ID does not exist.
     *
     * REQ-CRT-001
     */
    @PostMapping("/items")
    public ResponseEntity<CartItemDTO> addItem(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddToCartRequestDTO request) {
        return ResponseEntity.ok(cartService.addItem(currentUser.getId(), request));
    }

    /**
     * PUT /api/cart/items/{itemId}
     *
     * Updates the quantity of a specific cart item.
     * Returns 400 if quantity < 1 (use DELETE to remove).
     * Returns 404 if the item does not exist in the user's cart.
     *
     * REQ-CRT-003 AC1
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItemDTO> updateItem(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequestDTO request) {
        return ResponseEntity.ok(
                cartService.updateItem(currentUser.getId(), itemId, request));
    }

    /**
     * DELETE /api/cart/items/{itemId}
     *
     * Removes a specific item from the cart entirely.
     * Returns 204 No Content on success.
     * Returns 404 if the item does not exist in the user's cart.
     *
     * REQ-CRT-003 AC2
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long itemId) {
        cartService.deleteItem(currentUser.getId(), itemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/cart/merge
     *
     * Merges the guest localStorage cart into the server cart after login.
     * Duplicate books have their quantities summed.
     * Returns the full merged cart.
     *
     * REQ-CRT-005, REQ-USR-003
     */
    @PostMapping("/merge")
    public ResponseEntity<CartResponseDTO> mergeCart(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody MergeCartRequestDTO request) {
        return ResponseEntity.ok(cartService.merge(currentUser.getId(), request));
    }
}
