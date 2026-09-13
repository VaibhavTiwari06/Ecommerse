package com.ebookstore.backend.book;

import com.ebookstore.backend.book.dto.BookDetailDTO;
import com.ebookstore.backend.book.dto.BookFilterParams;
import com.ebookstore.backend.book.dto.BookSummaryDTO;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * BookService — orchestrates all book catalogue operations.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-004 through TASK-CAT-007)
 * =============================================================
 * Provides:
 *
 *  browse(params)     — paginated, filtered catalogue list (REQ-CAT-001, REQ-SRC-002)
 *  getDetail(id)      — single book with delivery date (REQ-CAT-004)
 *  getRelated(id)     — same-category books, up to 8 (REQ-CAT-005)
 *  search(term, size) — full-text search (REQ-SRC-001, CR-002)
 *
 * SEARCH PROFILE DETECTION:
 *   In production (PostgreSQL) we use the native tsvector query.
 *   In tests (H2, @ActiveProfiles("test")) we use a simple JPQL ILIKE
 *   fallback. The active environment is inspected at runtime via
 *   Spring's Environment bean.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    /** Maximum books returned from a search query. */
    private static final int MAX_SEARCH_RESULTS = 50;

    /** Related books to return per request. */
    private static final int RELATED_BOOKS_LIMIT = 8;

    private final BookRepository bookRepository;
    private final Environment environment;

    public BookService(BookRepository bookRepository, Environment environment) {
        this.bookRepository = bookRepository;
        this.environment = environment;
    }

    // ------------------------------------------------------------------
    // Browse (TASK-CAT-004, REQ-CAT-001, REQ-SRC-002)
    // ------------------------------------------------------------------

    /**
     * Returns a paginated, filtered page of books.
     *
     * All filter parameters are optional — null means "no restriction".
     * Pagination and sort defaults are applied by BookFilterParams.
     *
     * @param params filter/page/sort parameters from the request
     * @return page of BookSummaryDTO
     */
    public Page<BookSummaryDTO> browse(BookFilterParams params) {

        // Validate and cap page size to prevent abuse
        int size = Math.min(Math.max(params.size(), 1), 100);
        int page = Math.max(params.page(), 0);

        Sort sort = buildSort(params.sort(), params.dir());
        PageRequest pageable = PageRequest.of(page, size, sort);

        boolean inStock = Boolean.TRUE.equals(params.inStock());

        // Pass empty string (not null) for name filters so JPQL
        // ":name = ''" sentinel works correctly with Hibernate.
        // CR-005/BUG-002b
        String categoryName  = params.category()  != null ? params.category()  : "";
        String publisherName = params.publisher()  != null ? params.publisher() : "";

        Page<Book> books = bookRepository.findFiltered(
                params.categoryId(),
                params.publisherId(),
                params.minPrice(),
                params.maxPrice(),
                inStock,
                categoryName,
                publisherName,
                pageable);

        return books.map(this::toSummary);
    }

    // ------------------------------------------------------------------
    // Detail (TASK-CAT-005, REQ-CAT-004)
    // ------------------------------------------------------------------

    /**
     * Returns full book details including tentative delivery date.
     *
     * @param id the book ID
     * @return BookDetailDTO
     * @throws EntityNotFoundException if book not found (→ 404)
     */
    public BookDetailDTO getDetail(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: " + id));
        return toDetail(book);
    }

    // ------------------------------------------------------------------
    // Related books (TASK-CAT-006, REQ-CAT-005)
    // ------------------------------------------------------------------

    /**
     * Returns up to 8 books in the same category as the given book,
     * excluding the book itself.
     *
     * @param bookId the book whose related books are requested
     * @return list of BookSummaryDTO (may be empty if no related books)
     */
    public List<BookSummaryDTO> getRelated(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: " + bookId));

        PageRequest pageable = PageRequest.of(0, RELATED_BOOKS_LIMIT);
        return bookRepository.findRelated(
                        book.getCategory().getId(), bookId, pageable)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    // ------------------------------------------------------------------
    // Search (TASK-CAT-007, REQ-SRC-001, CR-002)
    // ------------------------------------------------------------------

    /**
     * Searches books by the given term.
     *
     * In production (PostgreSQL): uses tsvector full-text search + ts_rank.
     * In tests (H2): uses JPQL ILIKE fallback via searchBooksH2().
     *
     * The search term is sanitised before being passed to to_tsquery():
     *   - Stripped of special tsquery characters
     *   - Split into tokens joined with " & " (AND)
     *   Example: "harry potter" → "harry & potter"
     *
     * @param term the raw search term entered by the user
     * @param size max results to return (capped at MAX_SEARCH_RESULTS)
     * @return list of BookSummaryDTO ordered by relevance
     */
    public List<BookSummaryDTO> search(String term, int size) {
        if (term == null || term.isBlank()) {
            return List.of();
        }

        int limit = Math.min(size, MAX_SEARCH_RESULTS);
        String cleanTerm = term.trim();

        // Use H2 fallback in test profile
        if (isTestProfile()) {
            PageRequest pageable = PageRequest.of(0, limit);
            return bookRepository.searchBooksH2(cleanTerm, pageable)
                    .stream()
                    .map(this::toSummary)
                    .toList();
        }

        // PostgreSQL full-text search (production)
        String tsQuery  = buildTsQuery(cleanTerm);
        String rawTerm  = "%" + cleanTerm + "%";

        log.debug("Book search: term='{}' tsQuery='{}'", cleanTerm, tsQuery);

        return bookRepository.searchBooks(tsQuery, rawTerm, limit)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Checks if the "test" Spring profile is active.
     * Used to switch between PostgreSQL tsvector search and H2 ILIKE fallback.
     */
    private boolean isTestProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

    /**
     * Converts a raw search term into a PostgreSQL tsquery string.
     *
     * Steps:
     *   1. Strip characters that have special meaning in tsquery
     *      (&, |, !, :, (, ), ', <, >, *)
     *   2. Split on whitespace into tokens
     *   3. Filter out blank tokens
     *   4. Join with " & " (AND — all terms must match)
     *
     * Example: "harry's potter!" → "harry & potter"
     */
    private static String buildTsQuery(String term) {
        String sanitised = term.replaceAll("[&|!:()<>'*]", " ");
        String[] tokens  = sanitised.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!token.isBlank()) {
                if (!sb.isEmpty()) sb.append(" & ");
                sb.append(token);
            }
        }
        return sb.isEmpty() ? "unknown" : sb.toString();
    }

    /**
     * Builds a Spring Data Sort from the sort field name and direction.
     * Defaults to createdAt DESC for any unrecognised field.
     */
    private static Sort buildSort(String sortField, String dir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        String field = switch (sortField == null ? "" : sortField.toLowerCase()) {
            case "title"     -> "title";
            case "price"     -> "price";
            case "createdat" -> "createdAt";
            default          -> "createdAt";
        };

        return Sort.by(direction, field);
    }

    /**
     * Maps a Book entity to a BookSummaryDTO.
     * Used for catalogue list and related books.
     */
    private BookSummaryDTO toSummary(Book book) {
        return new BookSummaryDTO(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthors(),
                book.getCoverImageUrl(),
                book.getPrice(),
                book.getCategory().getName(),
                book.getPublisher().getName(),
                book.getStockQuantity() > 0
        );
    }

    /**
     * Maps a Book entity to a BookDetailDTO.
     * Includes tentative delivery date (today + 5 business days).
     */
    private BookDetailDTO toDetail(Book book) {
        return new BookDetailDTO(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthors(),
                book.getDescription(),
                book.getCoverImageUrl(),
                book.getPrice(),
                book.getCategory().getName(),
                book.getCategory().getId(),
                book.getPublisher().getName(),
                book.getPublisher().getId(),
                book.getPublishedDate(),
                book.getPageCount(),
                book.getLanguage(),
                book.getStockQuantity() > 0,
                book.getStockQuantity(),
                DeliveryDateCalculator.tentativeDeliveryDate()
        );
    }
}
