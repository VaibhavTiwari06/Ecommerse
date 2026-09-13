package com.ebookstore.backend.cart;

import com.ebookstore.backend.book.Book;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * CartItem — JPA entity mapping to the `cart_items` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CRT-001, REQ-CRT-001)
 * =============================================================
 * Represents one line item in a user's cart: a specific book
 * with a quantity. There is at most one CartItem per (cart, book)
 * — the UNIQUE constraint in V7__cart.sql enforces this.
 * Adding the same book again increments quantity (REQ-CRT-001 AC2).
 *
 * RULE: quantity must always be >= 1.
 *       Enforced by DB CHECK constraint and by CartService validation.
 *       To remove a book, DELETE the CartItem — never set quantity to 0.
 */
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The cart this item belongs to.
     * LAZY — cart data only loaded when explicitly accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * The book being purchased.
     * LAZY — book data loaded when needed for DTO mapping.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /**
     * Number of copies in the cart.
     * Must be >= 1 (DB CHECK + service validation).
     */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Timestamp when this item was first added to the cart. */
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    protected void onCreate() {
        this.addedAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public Long getId()                     { return id; }

    public Cart getCart()                   { return cart; }
    public void setCart(Cart cart)          { this.cart = cart; }

    public Book getBook()                   { return book; }
    public void setBook(Book book)          { this.book = book; }

    public int getQuantity()                { return quantity; }
    public void setQuantity(int quantity)   { this.quantity = quantity; }

    public Instant getAddedAt()             { return addedAt; }
}
