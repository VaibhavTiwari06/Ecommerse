package com.ebookstore.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * UserRepository — Spring Data JPA repository for the User entity.
 *
 * =============================================================
 * WHY THIS INTERFACE EXISTS (TASK-AUTH-001, REQ-USR-004, REQ-USR-005)
 * =============================================================
 * Spring Data JPA generates the SQL for standard CRUD operations
 * automatically from method names. We only need to define the
 * queries that aren't covered by the defaults.
 *
 * KEY QUERIES:
 *
 * findByEmail — used during login to look up a user by their email.
 *
 * findByPhoneNumber — used during login to look up a user by phone
 *   (CR-001: login accepts email OR phone).
 *
 * findByEmailOrPhoneNumber — the key login query. Finds a user where
 *   EITHER the email OR the phone_number matches the given identifier.
 *   The @Query uses JPQL (Java Persistence Query Language) rather than
 *   a derived method name because the OR condition on two different
 *   columns isn't expressible cleanly as a method name.
 *
 * existsByEmail / existsByPhoneNumber — efficient existence checks used
 *   during registration to detect duplicates BEFORE attempting the INSERT,
 *   giving us a clean 409 error instead of a constraint violation.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their exact email address.
     * Returns Optional.empty() if not found.
     * Used during registration duplicate check and profile lookup.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their exact phone number.
     * Returns Optional.empty() if not found.
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Find a user where email = :identifier OR phone_number = :identifier.
     *
     * This is the core login query (REQ-USR-005, CR-001).
     * The same value is passed for both parameters — JPQL handles the OR.
     *
     * Example: findByEmailOrPhoneNumber("test@test.com", "test@test.com")
     *   → SELECT * FROM users WHERE email = ? OR phone_number = ?
     *
     * @param email     the identifier to match against email column
     * @param phone     the identifier to match against phone_number column
     * @return Optional containing the user if found
     */
    @Query("SELECT u FROM User u WHERE u.email = :email OR u.phoneNumber = :phone")
    Optional<User> findByEmailOrPhoneNumber(
            @Param("email") String email,
            @Param("phone") String phone);

    /**
     * Returns true if any user has this email.
     * Used for duplicate email check during registration (REQ-USR-004 AC1).
     */
    boolean existsByEmail(String email);

    /**
     * Returns true if any user has this phone number.
     * Used for duplicate phone check during registration (REQ-USR-004 AC2).
     */
    boolean existsByPhoneNumber(String phoneNumber);
}
