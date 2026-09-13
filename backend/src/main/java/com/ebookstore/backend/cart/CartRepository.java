package com.ebookstore.backend.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * CartRepository — data access for the carts table.
 *
 * WHY THESE METHODS:
 *   findByUserId — used by CartService.getOrCreateCart() to look up
 *                  the existing cart for a user before deciding whether
 *                  to create a new one (REQ-CRT-001).
 *
 *   findByUserIdWithItems — eagerly fetches cart items and their books
 *                           in a single query, avoiding N+1 selects when
 *                           building CartResponseDTO (REQ-CRT-002).
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    /**
     * Fetches the cart with all items and their books in one query.
     * Used by GET /api/cart to avoid N+1 selects when mapping to DTO.
     */
    @Query("""
            SELECT c FROM Cart c
            LEFT JOIN FETCH c.items i
            LEFT JOIN FETCH i.book
            WHERE c.user.id = :userId
            """)
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);
}
