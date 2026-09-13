package com.ebookstore.backend.cart;

import com.ebookstore.backend.book.Book;
import com.ebookstore.backend.book.BookRepository;
import com.ebookstore.backend.cart.dto.*;
import com.ebookstore.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * CartService — all cart business logic.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CRT-002, TASK-CRT-003, TASK-CRT-005)
 * =============================================================
 * Provides:
 *   getCart(userId)              — view cart (REQ-CRT-002)
 *   addItem(userId, request)     — add/upsert item (REQ-CRT-001)
 *   updateItem(userId, id, req)  — change quantity (REQ-CRT-003 AC1)
 *   deleteItem(userId, id)       — remove item (REQ-CRT-003 AC2)
 *   merge(userId, request)       — merge guest cart on login (REQ-CRT-005)
 *
 * KEY RULES:
 *   - getOrCreateCart(): every authenticated user gets a cart on first use.
 *   - Upsert on addItem(): same book → increment, new book → insert.
 *   - Out-of-stock check: adding a book with stockQuantity = 0 → 400.
 *   - Ownership check: PUT/DELETE verify the item belongs to the user's cart.
 *   - Delivery charge: always ₹40 (REQ-CHK-002, Decision OQ-001).
 *   - Merge: duplicate bookId → sum quantities; new bookId → insert.
 */
@Service
@Transactional
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    /** Fixed delivery charge per order (REQ-CHK-002, Decision OQ-001). */
    private static final BigDecimal DELIVERY_CHARGE = BigDecimal.valueOf(40);

    private final CartRepository     cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository     bookRepository;
    private final UserRepository     userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       BookRepository bookRepository,
                       UserRepository userRepository) {
        this.cartRepository     = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository     = bookRepository;
        this.userRepository     = userRepository;
    }

    // ------------------------------------------------------------------
    // View cart (TASK-CRT-003, REQ-CRT-002)
    // ------------------------------------------------------------------

    /**
     * Returns the full cart for the authenticated user.
     * Creates an empty cart if the user has none yet.
     * REQ-CRT-002 AC3: empty cart returns empty items list, not 404.
     *
     * @param userId authenticated user's ID
     * @return CartResponseDTO with items, subtotal, delivery charge, grand total
     */
    @Transactional(readOnly = true)
    public CartResponseDTO getCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElse(null);
        if (cart == null) {
            // No cart yet → return empty cart response (REQ-CRT-002 AC3)
            return emptyCartResponse();
        }
        return toCartResponse(cart);
    }

    // ------------------------------------------------------------------
    // Add item (TASK-CRT-002, REQ-CRT-001)
    // ------------------------------------------------------------------

    /**
     * Adds a book to the cart, or increments its quantity if already present.
     *
     * REQ-CRT-001 AC1: POST /api/cart/items adds the book.
     * REQ-CRT-001 AC2: Same book again → quantity incremented, not new row.
     * REQ-CRT-001 AC3: Out-of-stock book → 400 (IllegalArgumentException).
     *
     * @param userId  authenticated user's ID
     * @param request bookId + quantity to add
     * @return the added/updated CartItemDTO
     */
    public CartItemDTO addItem(Long userId, AddToCartRequestDTO request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Book not found: " + request.bookId()));

        // REQ-CRT-001 AC3 — out-of-stock check
        if (book.getStockQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "'" + book.getTitle() + "' is currently out of stock");
        }

        Cart cart = getOrCreateCart(userId);

        // Upsert: if book already in cart, increment quantity (REQ-CRT-001 AC2)
        CartItem item = cartItemRepository
                .findByCartIdAndBookId(cart.getId(), book.getId())
                .orElse(null);

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.quantity());
            cartItemRepository.save(item);
        } else {
            item = new CartItem();
            item.setCart(cart);
            item.setBook(book);
            item.setQuantity(request.quantity());
            cartItemRepository.save(item);
        }

        log.debug("Cart item upserted: userId={} bookId={} qty={}",
                userId, book.getId(), item.getQuantity());
        return toItemDTO(item);
    }

    // ------------------------------------------------------------------
    // Update item (TASK-CRT-002, REQ-CRT-003 AC1)
    // ------------------------------------------------------------------

    /**
     * Updates the quantity of a specific cart item.
     * Validates ownership — item must belong to the user's cart.
     *
     * REQ-CRT-003 AC1: PUT /api/cart/items/{itemId} updates quantity.
     * REQ-CRT-003 AC3: quantity < 1 → 400 (enforced by @Min on DTO).
     *
     * @param userId     authenticated user's ID
     * @param itemId     the cart item to update
     * @param request    new quantity
     * @return the updated CartItemDTO
     */
    public CartItemDTO updateItem(Long userId, Long itemId,
                                  UpdateCartItemRequestDTO request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository
                .findByCartIdAndId(cart.getId(), itemId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cart item not found: " + itemId));

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        log.debug("Cart item updated: itemId={} newQty={}", itemId, request.quantity());
        return toItemDTO(item);
    }

    // ------------------------------------------------------------------
    // Delete item (TASK-CRT-002, REQ-CRT-003 AC2)
    // ------------------------------------------------------------------

    /**
     * Removes a specific item from the cart.
     * Validates ownership — item must belong to the user's cart.
     *
     * REQ-CRT-003 AC2: DELETE /api/cart/items/{itemId} removes the item.
     *
     * @param userId authenticated user's ID
     * @param itemId the cart item to remove
     */
    public void deleteItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository
                .findByCartIdAndId(cart.getId(), itemId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cart item not found: " + itemId));

        cartItemRepository.delete(item);
        log.debug("Cart item deleted: itemId={} userId={}", itemId, userId);
    }

    // ------------------------------------------------------------------
    // Merge guest cart on login (TASK-CRT-005, REQ-CRT-005, REQ-USR-003)
    // ------------------------------------------------------------------

    /**
     * Merges the guest localStorage cart into the server cart after login.
     *
     * For each item in the request:
     *   - If book already in cart → sum quantities.
     *   - If book not in cart → insert new item.
     *   - If book is out of stock or not found → skip silently (don't fail merge).
     *
     * Returns the full updated cart after merge.
     *
     * REQ-CRT-005, REQ-USR-003
     *
     * @param userId  authenticated user's ID
     * @param request list of {bookId, quantity} from localStorage
     * @return full merged CartResponseDTO
     */
    public CartResponseDTO merge(Long userId, MergeCartRequestDTO request) {
        if (request.items().isEmpty()) {
            return getCart(userId);
        }

        Cart cart = getOrCreateCart(userId);

        for (MergeCartRequestDTO.MergeItem mergeItem : request.items()) {
            bookRepository.findById(mergeItem.bookId()).ifPresent(book -> {
                // Skip out-of-stock books silently during merge
                if (book.getStockQuantity() <= 0) {
                    log.debug("Merge: skipping out-of-stock book id={}", book.getId());
                    return;
                }

                cartItemRepository
                        .findByCartIdAndBookId(cart.getId(), book.getId())
                        .ifPresentOrElse(
                                existing -> {
                                    // Sum quantities (REQ-CRT-005 AC2)
                                    existing.setQuantity(
                                            existing.getQuantity() + mergeItem.quantity());
                                    cartItemRepository.save(existing);
                                },
                                () -> {
                                    CartItem newItem = new CartItem();
                                    newItem.setCart(cart);
                                    newItem.setBook(book);
                                    newItem.setQuantity(mergeItem.quantity());
                                    cartItemRepository.save(newItem);
                                }
                        );
            });
        }

        log.debug("Cart merge complete: userId={} itemsRequested={}",
                userId, request.items().size());

        // Flush pending inserts, then build the response directly from
        // cartItemRepository to bypass the Hibernate L1 cache, which would
        // otherwise return the stale Cart.items collection (TEST-CRT-006).
        cartItemRepository.flush();
        List<CartItem> mergedItems = cartItemRepository.findAllByCartId(cart.getId());
        return toCartResponseFromItems(mergedItems);
    }

    // ------------------------------------------------------------------
    // Package-visible helpers used by CheckoutService (M6)
    // ------------------------------------------------------------------

    /**
     * Returns or creates the cart for a user.
     * Called by all mutating operations and by CheckoutService.
     */
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    // getReferenceById returns a proxy — no SELECT issued,
                    // just sets the FK for the INSERT (avoids loading full User)
                    newCart.setUser(userRepository.getReferenceById(userId));
                    return cartRepository.save(newCart);
                });
    }

    // ------------------------------------------------------------------
    // Private mapping helpers
    // ------------------------------------------------------------------

    private CartResponseDTO toCartResponse(Cart cart) {
        return toCartResponseFromItems(cart.getItems());
    }

    private CartResponseDTO toCartResponseFromItems(List<CartItem> cartItems) {
        List<CartItemDTO> items = cartItems.stream()
                .map(this::toItemDTO)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemDTO::itemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = subtotal.add(DELIVERY_CHARGE);

        return new CartResponseDTO(items, subtotal, DELIVERY_CHARGE, grandTotal);
    }

    private CartResponseDTO emptyCartResponse() {
        return new CartResponseDTO(
                List.of(), BigDecimal.ZERO, DELIVERY_CHARGE, DELIVERY_CHARGE);
    }

    private CartItemDTO toItemDTO(CartItem item) {
        Book book = item.getBook();
        BigDecimal itemTotal = book.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemDTO(
                item.getId(),
                book.getId(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getPrice(),
                item.getQuantity(),
                itemTotal
        );
    }
}
