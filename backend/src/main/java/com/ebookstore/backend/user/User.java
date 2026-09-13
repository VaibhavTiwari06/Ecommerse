package com.ebookstore.backend.user;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * User — JPA entity mapping to the `users` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-001, REQ-USR-004, REQ-USR-005)
 * =============================================================
 * This is the central entity representing a registered customer.
 * It holds authentication credentials (email, phone, password hash)
 * and the gift point balance accumulated through purchases.
 *
 * IMPORTANT RULES:
 *   - passwordHash must ALWAYS be a BCrypt hash — never plaintext.
 *   - giftPointBalance must NEVER go below 0 (enforced by service +
 *     DB CHECK constraint).
 *   - This entity must NEVER be returned directly from a controller.
 *     Always convert to a DTO (UserProfileDTO, AuthResponseDTO) first.
 *
 * DESIGN NOTE:
 *   We use a simple Long id (BIGSERIAL in DB) rather than UUID
 *   to keep queries and joins simple. The JWT sub claim holds the id.
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * Auto-generated surrogate primary key.
     * GenerationType.IDENTITY lets PostgreSQL's BIGSERIAL handle it.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user's display name shown in the UI.
     * Not used for login — email and phone are the identifiers.
     */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Email address — one of the two login identifiers (CR-001).
     * Must be unique across all users.
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * 10-digit Indian mobile number — the second login identifier (CR-001).
     * Stored as a string to preserve format and avoid integer overflow.
     * Must be unique across all users.
     */
    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    /**
     * BCrypt hash of the user's password.
     * SECURITY: This is the ONLY form in which a password is stored.
     * BCrypt.matches() is used for verification — never plain comparison.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Running total of gift points earned through purchases.
     * Earn rule: floor(grandTotal / 50) per order (REQ-GFT-001).
     * Redeem rule: 1 point = ₹2 discount (REQ-GFT-002).
     * DB CHECK constraint ensures this never goes below 0.
     */
    @Column(name = "gift_point_balance", nullable = false)
    private int giftPointBalance = 0;

    /** Record creation timestamp — set once on INSERT, never updated. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Last update timestamp — refreshed on every UPDATE. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Lifecycle callbacks — automatically manage timestamps
    // ------------------------------------------------------------------

    /**
     * Called by JPA before the entity is first persisted (INSERT).
     * Sets both createdAt and updatedAt to the current UTC time.
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Called by JPA before the entity is updated (UPDATE).
     * Refreshes updatedAt to the current UTC time.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Getters and setters — no Lombok to keep the code explicit
    // ------------------------------------------------------------------

    public Long getId() { return id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getGiftPointBalance() { return giftPointBalance; }
    public void setGiftPointBalance(int giftPointBalance) { this.giftPointBalance = giftPointBalance; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
