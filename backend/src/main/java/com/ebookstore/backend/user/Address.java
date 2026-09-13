package com.ebookstore.backend.user;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Address — JPA entity mapping to the `addresses` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-AUTH-005, REQ-USR-006, REQ-CHK-001)
 * =============================================================
 * A registered user can save one or more delivery addresses.
 * During checkout, the user picks one of their saved addresses
 * as the delivery destination (REQ-CHK-001).
 *
 * RELATIONSHIP:
 *   Many addresses → one User.
 *   The @ManyToOne maps to users.id with a foreign key.
 *   If the user is deleted, all their addresses cascade-delete
 *   (defined in the DB migration, not here — JPA just models it).
 *
 * RULE (Decision OQ-002):
 *   A user can have multiple addresses. There is no hard limit in code.
 */
@Entity
@Table(name = "addresses")
public class Address {

    /** Auto-generated surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owns this address.
     * FetchType.LAZY means the User is only loaded from DB when
     * address.getUser() is called — avoids unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Optional friendly label shown in the UI.
     * Examples: "Home", "Office", "Parents' place"
     * Can be null — the UI falls back to showing the street address.
     */
    @Column(name = "label", length = 100)
    private String label;

    /** House number, building name, and street. */
    @Column(name = "street", nullable = false, length = 255)
    private String street;

    /** City name. */
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    /** Indian state name. */
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    /**
     * Indian postal code.
     * Stored as VARCHAR to handle leading zeros and special formats.
     */
    @Column(name = "pincode", nullable = false, length = 10)
    private String pincode;

    /**
     * Whether this is the user's default address.
     * The frontend highlights it during checkout for convenience.
     * Multiple addresses can technically have isDefault=true in the DB,
     * but the service only marks one at a time.
     */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /** Record creation timestamp. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Set createdAt before first INSERT. */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public Instant getCreatedAt() { return createdAt; }
}
