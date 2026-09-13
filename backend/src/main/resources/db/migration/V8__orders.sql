-- =============================================================
-- V8__orders.sql
-- Migration: V8 — Create orders and order_items tables
-- Milestone: M5 (TASK-ORD-001)
-- Requirements: REQ-ORD-001, REQ-ORD-002, REQ-ORD-003, REQ-ORD-004
-- =============================================================
--
-- PRICE / TITLE SNAPSHOTS:
--   order_items.title_snapshot and price_snapshot are copied from
--   books at moment of order creation. Order history always reflects
--   what the user actually paid, even if book data changes later.
--
-- STATUS LIFECYCLE:
--   CONFIRMED → PROCESSING → OUT_FOR_DELIVERY → DELIVERED
--   CONFIRMED or PROCESSING → CANCELLED  (REQ-ORD-004)
--
-- GIFT POINTS:
--   gift_points_redeemed: points spent (0 if none used)
--   gift_point_discount : monetary value = points × 2  (Decision D-008)
-- =============================================================

CREATE TABLE orders (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          NOT NULL REFERENCES users(id),
    address_id              BIGINT          NOT NULL REFERENCES addresses(id),
    status                  VARCHAR(30)     NOT NULL DEFAULT 'CONFIRMED',
    subtotal                NUMERIC(10,2)   NOT NULL,
    delivery_charge         NUMERIC(10,2)   NOT NULL DEFAULT 40.00,
    gift_points_redeemed    INTEGER         NOT NULL DEFAULT 0,
    gift_point_discount     NUMERIC(10,2)   NOT NULL DEFAULT 0.00,
    grand_total             NUMERIC(10,2)   NOT NULL,
    tentative_delivery_date DATE            NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);

CREATE TABLE order_items (
    id              BIGSERIAL      PRIMARY KEY,
    order_id        BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id         BIGINT         NOT NULL REFERENCES books(id),
    title_snapshot  VARCHAR(500)   NOT NULL,
    price_snapshot  NUMERIC(10,2)  NOT NULL,
    quantity        INTEGER        NOT NULL CHECK (quantity >= 1)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
