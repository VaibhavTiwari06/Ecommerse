package com.ebookstore.backend.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * OrderRepository — data access for the `orders` table.
 *
 * =============================================================
 * WHY THESE METHODS (TASK-ORD-001, REQ-ORD-002, design §2.7)
 * =============================================================
 *
 * findByUserIdOrderByCreatedAtDesc
 *   — Returns all orders for a user, most recent first (REQ-ORD-002 AC1).
 *     JOIN FETCH items so we can compute itemCount in one query.
 *
 * findByIdWithItems
 *   — Loads a single order with its items eagerly for the detail/cancel/
 *     buy-again operations that need both the order and its items.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * All orders for a user, newest first.
     * JOIN FETCH items avoids N+1 when building OrderSummaryDTOs.
     * REQ-ORD-002 AC1
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.user.id = :userId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Single order with items fetched — used by cancel, buy-again, and detail.
     * Returns Optional.empty() if the order does not exist.
     */
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.book
            WHERE o.id = :orderId
            """)
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
}
