package com.ebookstore.backend.recommendation;

import com.ebookstore.backend.auth.JwtService;
import com.ebookstore.backend.book.dto.BookSummaryDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RecommendationController — GET /api/recommendations
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-REC-002, REQ-REC-002, design §2.11)
 * =============================================================
 * Single endpoint used by two frontend pages with different limits:
 *   Home/Catalogue  → ?limit=8  (default)
 *   Basket page     → ?limit=4
 *
 * Returns 200 with an empty list when the user has no order history.
 * The frontend hides the recommendation section in that case.
 * Requires a valid JWT (protected by SecurityConfig catch-all).
 *
 * REQ-REC-001 AC1, AC4
 * REQ-REC-002 AC1, AC2
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtService            jwtService;

    public RecommendationController(RecommendationService recommendationService,
                                    JwtService jwtService) {
        this.recommendationService = recommendationService;
        this.jwtService            = jwtService;
    }

    /**
     * GET /api/recommendations?limit=8
     *
     * Returns up to `limit` recommended books for the authenticated user,
     * ordered by relevance score (same category = +2, same author = +1).
     *
     * @param authHeader  Bearer JWT from request header
     * @param limit       max books to return (default 8; use 4 for basket page)
     * @return 200 with list of BookSummaryDTO (empty list if no order history)
     */
    @GetMapping
    public ResponseEntity<List<BookSummaryDTO>> getRecommendations(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "8") int limit) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        List<BookSummaryDTO> recs = recommendationService.getRecommendations(userId, limit);
        return ResponseEntity.ok(recs);
    }
}
