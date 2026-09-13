-- =============================================================
-- V7__cart.sql
-- Migration: V7 — Create carts and cart_items tables
-- Milestone: M4 (TASK-CRT-001)
-- Requirements: REQ-CRT-001, REQ-CRT-002, REQ-CRT-003, REQ-CRT-005
-- =============================================================
--
-- WHY THESE TABLES:
--   Every registered user has exactly one cart (enforced by UNIQUE
--   on user_id). The cart holds 1..N cart_items, each pointing to
--   a book. There is at most one row per (cart_id, book_id) — adding
--   the same book again increments quantity instead of inserting
--   a new row (REQ-CRT-001 AC2).
--
-- CONSTRAINTS:
--   carts.user_id          UNIQUE  — one cart per user
--   cart_items.(cart_id, book_id)  UNIQUE — one row per book per cart
--   cart_items.quantity    CHECK >= 1 — quantity can never be 0 or negative
--
-- CASCADE:
--   carts      ON DELETE CASCADE from users  — cart deleted when user deleted
--   cart_items ON DELETE CASCADE from carts  — items deleted when cart deleted
-- =============================================================

CREATE TABLE carts (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_items (
    id         BIGSERIAL   PRIMARY KEY,
    cart_id    BIGINT      NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    book_id    BIGINT      NOT NULL REFERENCES books(id),
    quantity   INTEGER     NOT NULL CHECK (quantity >= 1),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (cart_id, book_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
