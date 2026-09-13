package com.ebookstore.backend.book.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * PagedResponseDTO — a JSON-serialisable page envelope.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CR005-002, BUG-002a)
 * =============================================================
 * Spring's built-in Page<T> serialises the current page index
 * as "number" (not "page"). The frontend PagedResponse<T>
 * interface expects a "page" field. This mismatch caused the
 * pagination "Next" button to always request page 0.
 *
 * This DTO wraps a Spring Page with an explicit "page" field
 * so the frontend receives the correct shape:
 * {
 *   "content":       [...],
 *   "page":          0,
 *   "size":          20,
 *   "totalElements": 113,
 *   "totalPages":    6
 * }
 *
 * REQ-CAT-001, CR-005/BUG-002a
 */
public record PagedResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /** Factory — builds a PagedResponseDTO from any Spring Page. */
    public static <T> PagedResponseDTO<T> from(Page<T> springPage) {
        return new PagedResponseDTO<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
        );
    }
}
