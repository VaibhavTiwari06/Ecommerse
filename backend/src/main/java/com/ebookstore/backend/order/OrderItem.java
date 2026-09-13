package com.ebookstore.backend.order;

import com.ebookstore.backend.book.Book;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * OrderItem — JPA entity mapping to the `order_items` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-ORD-001, REQ-ORD-001 AC3, design §2.7)
 * =============================================================
 * Each row represents one distinct book line in an order.
 *
 * SNAPSHOT FIELDS:
 *   titleSnapshot  — book title at time of purchase
 *   priceSnapshot  — book price (₹) at time of purchase
 *
 * These fields are critical: if a book's title or price changes
 * after purchase the order history must still show what the user
 * actually paid for. We copy the values from the Book entity at
 * order creation time and never update them afterwards.
 *
 * book_id is kept as a FK so the "Buy Again" feature can look
 * up the book's current stock status (REQ-ORD-003 AC2).
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent order.
     * LAZY — individual items are only loaded when explicitly accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * FK to the book — used by Buy Again to check current stock.
     * Not used for price/title display (use snapshots for those).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /** Title captured at order creation — immutable after insert. */
    @Column(name = "title_snapshot", nullable = false, length = 500)
    private String titleSnapshot;

    /** Price per unit (₹) captured at order creation — immutable after insert. */
    @Column(name = "price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceSnapshot;

    /** Number of copies ordered. Always ≥ 1 (CHECK constraint in DB). */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public Long getId() { return id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public String getTitleSnapshot() { return titleSnapshot; }
    public void setTitleSnapshot(String titleSnapshot) { this.titleSnapshot = titleSnapshot; }

    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(BigDecimal priceSnapshot) { this.priceSnapshot = priceSnapshot; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
