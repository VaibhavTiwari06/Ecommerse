package com.ebookstore.backend.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * CartItemRepository — data access for the cart_items table.
 *
 * WHY THESE METHODS:
 *   findByCartIdAndBookId — used by CartService to check if a book is
 *                           already in the cart before adding it (upsert
 *                           logic — REQ-CRT-001 AC2).
 *                           Also used by CartService.merge() to detect
 *                           duplicates when merging the guest cart.
 *
 *   findByCartIdAndId    — used when updating/deleting an item to verify
 *                          the item belongs to the requesting user's cart
 *                          (ownership check — REQ-CRT-003).
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndBookId(Long cartId, Long bookId);

    Optional<CartItem> findByCartIdAndId(Long cartId, Long id);

    /**
     * Used by CartService.merge() to build the response after inserting new items,
     * bypassing the Hibernate L1 cache that would otherwise return the stale Cart.
     */
    List<CartItem> findAllByCartId(Long cartId);
}
