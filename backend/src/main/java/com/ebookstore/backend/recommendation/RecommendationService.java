package com.ebookstore.backend.recommendation;

import com.ebookstore.backend.book.Book;
import com.ebookstore.backend.book.BookRepository;
import com.ebookstore.backend.book.dto.BookSummaryDTO;
import com.ebookstore.backend.order.OrderItem;
import com.ebookstore.backend.order.OrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RecommendationService — rule-based book recommendation engine.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-REC-001, REQ-REC-001, design §2.11)
 * =============================================================
 *
 * Algorithm (spec §11, design §2.11):
 *  1. Load all OrderItems for the user across all past orders.
 *  2. If none → return empty list (no order history).
 *  3. Build sets:
 *       purchasedBookIds   — exclude these from recommendations
 *       purchasedCategoryIds — books in same category score +2
 *       purchasedAuthors   — primary author (before first comma) in same
 *                            author set scores +1
 *  4. Load ALL books NOT in purchasedBookIds.
 *  5. Score each:
 *       score = (categoryId in purchasedCategoryIds ? 2 : 0)
 *             + (primaryAuthor in purchasedAuthors  ? 1 : 0)
 *  6. Keep only score > 0, sort by score DESC then createdAt DESC.
 *  7. Return top `limit` as BookSummaryDTO.
 *
 * WHY IN-JAVA SCORING (not native SQL CASE):
 *   The test profile uses H2 which doesn't support PostgreSQL-specific
 *   CASE-in-WHERE SQL. Doing the scoring in Java keeps the query
 *   dialect-neutral (standard JPQL) and the logic easily testable.
 *   The candidate set (books not yet purchased) is small enough that
 *   fetching all and scoring in memory is fine.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final OrderItemRepository orderItemRepository;
    private final BookRepository      bookRepository;

    public RecommendationService(OrderItemRepository orderItemRepository,
                                 BookRepository bookRepository) {
        this.orderItemRepository = orderItemRepository;
        this.bookRepository      = bookRepository;
    }

    /**
     * Returns up to `limit` book recommendations for the given user.
     *
     * Returns an empty list (not an error) when the user has no order history.
     * REQ-REC-001 AC4
     *
     * @param userId authenticated user's ID
     * @param limit  maximum number of recommendations to return (default 8)
     * @return list of BookSummaryDTO ordered by relevance score
     */
    public List<BookSummaryDTO> getRecommendations(Long userId, int limit) {

        // Step 1 — all order items for this user
        List<OrderItem> purchased = orderItemRepository.findByOrderUserId(userId);

        // Step 2 — no history → empty list (REQ-REC-001 AC4)
        if (purchased.isEmpty()) {
            log.debug("Recommendations: no order history for userId={}", userId);
            return List.of();
        }

        // Step 3 — build exclusion + scoring sets
        Set<Long> purchasedBookIds = purchased.stream()
                .map(oi -> oi.getBook().getId())
                .collect(Collectors.toSet());

        Set<Long> purchasedCategoryIds = purchased.stream()
                .map(oi -> oi.getBook().getCategory().getId())
                .collect(Collectors.toSet());

        // Primary author = everything before the first comma (or the whole string)
        Set<String> purchasedAuthors = purchased.stream()
                .map(oi -> primaryAuthor(oi.getBook().getAuthors()))
                .collect(Collectors.toSet());

        log.debug("Recommendations: userId={} purchasedBooks={} categories={} authors={}",
                userId, purchasedBookIds.size(),
                purchasedCategoryIds.size(), purchasedAuthors.size());

        // Step 4 — candidate books: not purchased, in-stock only (REQ-REC-001 AC5, CR-005/BUG-003)
        List<Book> candidates = bookRepository.findCandidatesExcluding(purchasedBookIds)
                .stream()
                .filter(b -> b.getStockQuantity() > 0)
                .toList();

        // Step 5–7 — score, filter, sort, limit
        return candidates.stream()
                .map(b -> new ScoredBook(b, score(b, purchasedCategoryIds, purchasedAuthors)))
                .filter(sb -> sb.score > 0)
                .sorted(Comparator
                        .comparingInt(ScoredBook::score).reversed()
                        .thenComparing(sb -> sb.book.getCreatedAt(),
                                Comparator.reverseOrder()))
                .limit(limit)
                .map(sb -> toSummary(sb.book))
                .toList();
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private int score(Book book,
                      Set<Long> categoryIds,
                      Set<String> authors) {
        int s = 0;
        if (categoryIds.contains(book.getCategory().getId())) s += 2;
        if (authors.contains(primaryAuthor(book.getAuthors())))      s += 1;
        return s;
    }

    /**
     * Extracts the primary author — everything before the first comma.
     * "Frank Herbert, Brian Herbert" → "Frank Herbert"
     * "J.K. Rowling"                 → "J.K. Rowling"
     */
    private static String primaryAuthor(String authors) {
        if (authors == null) return "";
        int comma = authors.indexOf(',');
        return (comma >= 0 ? authors.substring(0, comma) : authors).trim();
    }

    private BookSummaryDTO toSummary(Book b) {
        return new BookSummaryDTO(
                b.getId(),
                b.getIsbn(),
                b.getTitle(),
                b.getAuthors(),
                b.getCoverImageUrl(),
                b.getPrice(),
                b.getCategory().getName(),
                b.getPublisher().getName(),
                b.getStockQuantity() > 0
        );
    }

    /** Tiny value object carrying a book and its computed score. */
    private record ScoredBook(Book book, int score) {}
}
