-- =============================================================
-- V5__publishers.sql
-- Migration: V5 — Create publishers table
-- Milestone: M3 (TASK-CAT-001)
-- Requirements: REQ-CAT-003
-- =============================================================
--
-- WHY THIS TABLE:
--   Books belong to a publisher (e.g. Penguin, HarperCollins).
--   "Brand" browsing (BR-003) allows users to browse by publisher.
--   Publishers are created by the BookLoader at startup from books.json.
--
-- DESIGN CHOICE:
--   Simple id + name. Publisher names can be up to 255 chars to
--   accommodate long academic or imprint names in the seed data.
--   The BookLoader calls PublisherService.findOrCreate(name).
-- =============================================================

CREATE TABLE publishers (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(255) NOT NULL UNIQUE
);
