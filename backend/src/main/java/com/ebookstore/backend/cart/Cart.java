package com.ebookstore.backend.cart;

import com.ebookstore.backend.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart — JPA entity mapping to the `carts` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CRT-001, REQ-CRT-001)
 * =============================================================
 * Every registered user has exactly one cart.
 * The UNIQUE constraint on user_id (in V7__cart.sql) enforces this
 * at the database level. CartService.getOrCreateCart() enforces it
 * at the application level.
 *
 * RELATIONSHIP:
 *   One Cart → many CartItems (one-to-many, cascade ALL).
 *   Deleting a Cart deletes all its CartItems (DB CASCADE + JPA cascade).
 *
 * RULE: This entity must never leave the service layer.
 *       Always map to CartResponseDTO before returning from a controller.
 */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The owner of this cart.
     * UNIQUE at DB level — one cart per user.
     * LAZY fetch — user data only loaded when explicitly accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * All items currently in this cart.
     * CascadeType.ALL — saving/deleting the cart propagates to items.
     * orphanRemoval = true — removing an item from this list deletes it from DB.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    /** Record creation timestamp — set once on INSERT. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public Long getId()                     { return id; }

    public User getUser()                   { return user; }
    public void setUser(User user)          { this.user = user; }

    public List<CartItem> getItems()        { return items; }

    public Instant getCreatedAt()           { return createdAt; }
}
