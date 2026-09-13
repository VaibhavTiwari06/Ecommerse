package com.ebookstore.backend.book.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * BookSummaryDTO — lightweight book card data for catalogue listing pages.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-004, REQ-CAT-001)
 * =============================================================
 * Used by:
 *   GET /api/books           — paginated catalogue browse
 *   GET /api/books/{id}/related  — related books strip
 *   GET /api/recommendations — recommendation strip
 *
 * Contains only the fields needed to render a book card in the UI.
 * Full details (description, page count, etc.) are in BookDetailDTO.
 *
 * @JsonProperty on categoryName / publisherName ensures the JSON keys
 * are "category" and "publisher" — matching the frontend BookSummary
 * TypeScript interface. CR-005 field-name alignment fix.
 */
public record BookSummaryDTO(
        Long id,
        String isbn,
        String title,
        String authors,
        String coverImageUrl,
        BigDecimal price,
        @JsonProperty("category")  String categoryName,
        @JsonProperty("publisher") String publisherName,
        boolean inStock
) {}
