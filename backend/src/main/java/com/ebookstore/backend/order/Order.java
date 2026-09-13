package com.ebookstore.backend.order;

import com.ebookstore.backend.user.Address;
import com.ebookstore.backend.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Order — JPA entity mapping to the `orders` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-ORD-001, REQ-ORD-001, design §2.7)
 * =============================================================
 * An order is created when payment succeeds (REQ-ORD-001).
 * It captures a permanent record of the transaction:
 *   - which user bought
 *   - to which address
 *   - what items (via OrderItem — with price/title snapshots)
 *   - what totals were charged
 *   - the tentative delivery date
 *
 * IMMUTABLE FIELDS (set once at creation, never updated):
 *   subtotal, deliveryCharge, giftPointsRedeemed,
 *   giftPointDiscount, grandTotal, tentativeDeliveryDate
 *
 * MUTABLE FIELD:
 *   status — progresses through the lifecycle; can be set to
 *   CANCELLED if currently CONFIRMED or PROCESSING (REQ-ORD-004)
 *
 * PRICE SNAPSHOTS:
 *   Prices are NOT stored on this entity — they live on OrderItem
 *   (title_snapshot, price_snapshot) to allow book prices to change
 *   without affecting historical order data.
 *
 * GIFT POINTS (Decision D-008):
 *   giftPointDiscount = giftPointsRedeemed × 2 (1 pt = ₹2)
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who placed this order.
     * LAZY — we rarely need the full User when loading an order list.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Delivery address selected at checkout.
     * FK preserved — if the address is later deleted the FK row
     * still exists; we snapshot street/city/etc in the response DTO.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    /**
     * Line items — one per distinct book in the original cart.
     * CascadeType.ALL so items are persisted/removed with the order.
     * orphanRemoval keeps DB clean if items are ever detached.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Current lifecycle status.
     * Stored as STRING (EnumType.STRING) so DB value is human-readable
     * and safe against enum reordering.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.CONFIRMED;

    /** Sum of (price × qty) for all items — before discount or delivery. */
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /** Always ₹40 (Decision OQ-001, REQ-CHK-002). */
    @Column(name = "delivery_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryCharge;

    /** Gift points the user chose to redeem at checkout (0 if none). */
    @Column(name = "gift_points_redeemed", nullable = false)
    private int giftPointsRedeemed = 0;

    /** Monetary discount from gift points: giftPointsRedeemed × 2 (Decision D-008). */
    @Column(name = "gift_point_discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal giftPointDiscount = BigDecimal.ZERO;

    /** subtotal + deliveryCharge − giftPointDiscount */
    @Column(name = "grand_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal grandTotal;

    /** Estimated delivery date = order date + 5 business days (REQ-CHK-003). */
    @Column(name = "tentative_delivery_date", nullable = false)
    private LocalDate tentativeDeliveryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDeliveryCharge() { return deliveryCharge; }
    public void setDeliveryCharge(BigDecimal deliveryCharge) { this.deliveryCharge = deliveryCharge; }

    public int getGiftPointsRedeemed() { return giftPointsRedeemed; }
    public void setGiftPointsRedeemed(int giftPointsRedeemed) { this.giftPointsRedeemed = giftPointsRedeemed; }

    public BigDecimal getGiftPointDiscount() { return giftPointDiscount; }
    public void setGiftPointDiscount(BigDecimal giftPointDiscount) { this.giftPointDiscount = giftPointDiscount; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public LocalDate getTentativeDeliveryDate() { return tentativeDeliveryDate; }
    public void setTentativeDeliveryDate(LocalDate tentativeDeliveryDate) {
        this.tentativeDeliveryDate = tentativeDeliveryDate;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
