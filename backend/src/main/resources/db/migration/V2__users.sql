-- =============================================================
-- V2__users.sql
-- Migration: V2 — Create users table
-- Milestone: M2 (TASK-AUTH-001)
-- Requirements: REQ-USR-004, REQ-USR-005, REQ-NFR-001, CR-001
-- =============================================================
--
-- WHY THESE COLUMNS:
--
--   id               — synthetic primary key, BIGSERIAL auto-increments
--   full_name        — display name shown in UI
--   email            — used as login identifier (unique)
--   phone_number     — also a login identifier per CR-001 (unique, 10 digits)
--   password_hash    — BCrypt hash of the password, NEVER plaintext
--   gift_point_balance — running total of earned gift points (REQ-GFT-001)
--                        CHECK ensures it never goes below 0
--   created_at/updated_at — audit timestamps
--
-- SECURITY NOTES:
--   - password_hash stores only the BCrypt output (60 chars for BCrypt,
--     but VARCHAR(255) gives room for future algorithm changes)
--   - No plain-text password, no reversible encryption
--   - The CHECK constraint on gift_point_balance is a last-resort
--     DB-level guard; the service layer validates first
-- =============================================================

CREATE TABLE users (
    id                  BIGSERIAL       PRIMARY KEY,
    full_name           VARCHAR(150)    NOT NULL,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    phone_number        VARCHAR(15)     NOT NULL UNIQUE,
    password_hash       VARCHAR(255)    NOT NULL,
    gift_point_balance  INTEGER         NOT NULL DEFAULT 0
                            CHECK (gift_point_balance >= 0),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Index on email and phone for fast login lookups
-- (both are used as login identifiers, CR-001)
CREATE INDEX idx_users_email        ON users(email);
CREATE INDEX idx_users_phone_number ON users(phone_number);
