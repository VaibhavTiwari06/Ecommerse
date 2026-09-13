-- =============================================================
-- V4__categories.sql
-- Migration: V4 — Create categories table
-- Milestone: M3 (TASK-CAT-001)
-- Requirements: REQ-CAT-002
-- =============================================================
--
-- WHY THIS TABLE:
--   Books are grouped into categories (e.g. Fiction, Science, History).
--   A category has a unique name used for browsing and filtering.
--   Categories are created by the BookLoader at startup from books.json
--   — there is no admin UI to manage them.
--
-- DESIGN CHOICE:
--   Simple id + name only. No parent/child hierarchy needed.
--   The BookLoader calls CategoryService.findOrCreate(name) to
--   ensure no duplicate categories are inserted.
-- =============================================================

CREATE TABLE categories (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE
);
