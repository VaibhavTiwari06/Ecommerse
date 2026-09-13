package com.ebookstore.backend.book.dto;

/**
 * BookFilterParams — query parameters for GET /api/books.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-004, REQ-SRC-002)
 * Updated: TASK-CR005-003 — added category/publisher name filters
 * =============================================================
 * Used by BookController to bind all filter/pagination query params
 * into a single object, then passed to BookService.
 *
 * All fields are nullable — omitting a filter means "no restriction".
 *
 * FILTERS (REQ-SRC-002):
 *   category     — filter by category name (e.g. "Fiction") CR-005/BUG-002b
 *   publisher    — filter by publisher name (e.g. "Penguin") CR-005/BUG-002b
 *   categoryId   — filter by category FK (kept for internal use)
 *   publisherId  — filter by publisher FK (kept for internal use)
 *   minPrice     — minimum price (inclusive)
 *   maxPrice     — maximum price (inclusive)
 *   inStock      — if true, return only books with stock_quantity > 0
 *
 * PAGINATION:
 *   page — 0-based page index (default 0)
 *   size — page size (default 20, max 100)
 *
 * SORT:
 *   sort — field name: "title" | "price" | "createdAt" (default "createdAt")
 *   dir  — "asc" | "desc" (default "desc")
 */
public record BookFilterParams(
        String category,
        String publisher,
        Long categoryId,
        Long publisherId,
        java.math.BigDecimal minPrice,
        java.math.BigDecimal maxPrice,
        Boolean inStock,
        int page,
        int size,
        String sort,
        String dir
) {
    /** Canonical defaults used when params are omitted from the request. */
    public static BookFilterParams defaults() {
        return new BookFilterParams(null, null, null, null, null, null, null,
                0, 20, "createdAt", "desc");
    }
}
