package com.ebookstore.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AddressRepository — Spring Data JPA repository for the Address entity.
 *
 * =============================================================
 * WHY THIS INTERFACE EXISTS (TASK-AUTH-005, REQ-USR-006)
 * =============================================================
 * Provides database access for delivery addresses.
 *
 * The key query is findByUserId — returns all addresses belonging
 * to a specific user, used when:
 *   1. The user views their profile/addresses.
 *   2. The checkout page loads saved addresses for selection.
 */
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Find all addresses belonging to the given user, ordered by
     * creation time (oldest first = stable order in the UI).
     *
     * Spring Data JPA generates:
     *   SELECT * FROM addresses WHERE user_id = ? ORDER BY created_at ASC
     *
     * @param userId the authenticated user's ID
     * @return list of addresses (empty list if user has none)
     */
    List<Address> findByUserIdOrderByCreatedAtAsc(Long userId);
}
