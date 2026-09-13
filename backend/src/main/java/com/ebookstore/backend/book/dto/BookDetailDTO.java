package com.ebookstore.backend.book.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BookDetailDTO — full book information for the Book Detail page.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-005, REQ-CAT-004)
 * =============================================================
 * Used by:
 *   GET /api/books/{id}  — the book detail page
 *
 * Includes all BookSummaryDTO fields PLUS:
 *   description          — full synopsis
 *   publishedDate        — year string
 *   pageCount            — nullable
 *   language             — ISO code
 *   stockQuantity        — exact count (for display, e.g. "Only 3 left")
 *   tentativeDeliveryDate — today + 5 business days (REQ-CAT-004 AC4,
 *                           REQ-CHK-003)
 *
 * @JsonProperty on categoryName / publisherName serialises as "category"
 * and "publisher" to match the frontend BookDetail TypeScript interface.
 * CR-005 field-name alignment fix.
 */
public record BookDetailDTO(
        Long id,
        String isbn,
        String title,
        String authors,
        String description,
        String coverImageUrl,
        BigDecimal price,
        @JsonProperty("category")  String categoryName,
        Long categoryId,
        @JsonProperty("publisher") String publisherName,
        Long publisherId,
        String publishedDate,
        Integer pageCount,
        String language,
        boolean inStock,
        int stockQuantity,
        LocalDate tentativeDeliveryDate
) {}
