-- =============================================================
-- V3__addresses.sql
-- Migration: V3 — Create addresses table
-- Milestone: M2 (TASK-AUTH-005)
-- Requirements: REQ-USR-006, REQ-CHK-001
-- =============================================================
--
-- WHY THIS TABLE:
--   A registered user can save multiple delivery addresses (Decision OQ-002).
--   During checkout the user selects one of their saved addresses.
--
-- COLUMNS:
--   user_id     — FK to users. ON DELETE CASCADE means if the user is
--                 deleted, all their addresses are deleted automatically.
--   label       — optional friendly name: "Home", "Office", etc.
--   street      — house number + street name
--   city        — city name
--   state       — Indian state name
--   pincode     — 6-digit Indian postal code (stored as VARCHAR
--                 to preserve leading zeros if any)
--   is_default  — convenience flag; the frontend highlights the default
--                 address during checkout
--   created_at  — audit timestamp
-- =============================================================

CREATE TABLE addresses (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL
                    REFERENCES users(id) ON DELETE CASCADE,
    label       VARCHAR(100),
    street      VARCHAR(255)    NOT NULL,
    city        VARCHAR(100)    NOT NULL,
    state       VARCHAR(100)    NOT NULL,
    pincode     VARCHAR(10)     NOT NULL,
    is_default  BOOLEAN         NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Index for fast lookup of all addresses belonging to a user
CREATE INDEX idx_addresses_user_id ON addresses(user_id);
