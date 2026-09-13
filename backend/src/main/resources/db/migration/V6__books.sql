-- =============================================================
-- V6__books.sql
-- Migration: V6 — Create books table
-- Milestone: M3 (TASK-CAT-002)
-- Requirements: REQ-CAT-001, REQ-CAT-004, REQ-CAT-005,
--               REQ-CAT-006, REQ-SRC-001, REQ-SRC-002, CR-002
-- =============================================================
--
-- WHY THESE COLUMNS:
--   isbn             — unique identifier (ISBN-13 preferred)
--   title            — up to 500 chars for long subtitles
--   authors          — comma-separated list (seed data format)
--   description      — full book synopsis (TEXT — no length limit)
--   cover_image_url  — Open Library cover URL, up to 1000 chars
--   publisher_id     — FK to publishers.id (REQ-CAT-003)
--   published_date   — year string e.g. "2008" (from Open Library)
--   page_count       — nullable — not all books have page counts
--   language         — ISO 639-1 code, default 'en'
--   category_id      — FK to categories.id (REQ-CAT-002)
--   price            — in Indian Rupees, must be > 0 (REQ-NFR-002)
--   stock_quantity   — integer units in stock, must be >= 0
--   search_vector    — PostgreSQL TSVECTOR generated column (CR-002)
--                      weighted: title(A) > authors(B) > isbn(D)
--   created_at       — audit timestamp
--
-- SEARCH VECTOR (CR-002):
--   Generated always as a stored computed column.
--   The GIN index (books_search_idx) makes full-text search fast.
--   search_vector is maintained automatically by PostgreSQL on
--   INSERT and UPDATE — the application never writes to it.
--
-- NOTE ON H2 COMPATIBILITY:
--   This migration file runs only against the real PostgreSQL database.
--   Tests use H2 with Hibernate DDL (Flyway disabled) and the Book
--   entity maps search_vector as @Column(insertable=false, updatable=false)
--   so Hibernate ignores it in H2 mode.
-- =============================================================

CREATE TABLE books (
    id               BIGSERIAL        PRIMARY KEY,
    isbn             VARCHAR(13)      NOT NULL UNIQUE,
    title            VARCHAR(500)     NOT NULL,
    authors          TEXT             NOT NULL,
    description      TEXT             NOT NULL,
    cover_image_url  VARCHAR(1000)    NOT NULL,
    publisher_id     BIGINT           NOT NULL REFERENCES publishers(id),
    published_date   VARCHAR(10),
    page_count       INTEGER,
    language         VARCHAR(10)      NOT NULL DEFAULT 'en',
    category_id      BIGINT           NOT NULL REFERENCES categories(id),
    price            NUMERIC(10,2)    NOT NULL CHECK (price > 0),
    stock_quantity   INTEGER          NOT NULL DEFAULT 0
                         CHECK (stock_quantity >= 0),
    search_vector    TSVECTOR GENERATED ALWAYS AS (
                         setweight(to_tsvector('english', coalesce(title,   '')), 'A') ||
                         setweight(to_tsvector('english', coalesce(authors, '')), 'B') ||
                         setweight(to_tsvector('simple',  coalesce(isbn,    '')), 'D')
                     ) STORED,
    created_at       TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

-- GIN index for fast full-text search (CR-002)
CREATE INDEX books_search_idx   ON books USING GIN(search_vector);

-- Supporting B-tree indexes for filter and sort queries (REQ-SRC-002)
CREATE INDEX idx_books_category  ON books(category_id);
CREATE INDEX idx_books_publisher ON books(publisher_id);
CREATE INDEX idx_books_price     ON books(price);
CREATE INDEX idx_books_stock     ON books(stock_quantity);
