package com.ebookstore.backend.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * OrderItemRepository — data access for the `order_items` table.
 *
 * =============================================================
 * WHY THIS EXISTS (TASK-ORD-001, design §2.7)
 * =============================================================
 * Used by RecommendationService (M9) to find distinct book IDs
 * a user has purchased. All other order-item access goes through
 * OrderRepository.findByIdWithItems() which JOIN FETCHes items.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Returns all order items across all orders for a given user.
     * Used by RecommendationService (M9) to compute purchased book IDs.
     */
    List<OrderItem> findByOrderUserId(Long userId);
}
