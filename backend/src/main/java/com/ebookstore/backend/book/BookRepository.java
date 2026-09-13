package com.ebookstore.backend.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * BookRepository — data access for the books table.
 *
 * =============================================================
 * WHY THESE METHODS (TASK-CAT-004 through TASK-CAT-007)
 * =============================================================
 *
 * findAll(Pageable)  — inherited from JpaRepository; used for
 *                       unfiltered paginated catalogue browse.
 *
 * findFiltered()     — dynamic filter query using JPQL.
 *                       Handles optional category, publisher, price
 *                       range, and inStock filters (REQ-SRC-002).
 *                       Uses (:param IS NULL OR ...) pattern so any
 *                       null parameter is treated as "no filter".
 *
 * findRelated()      — finds up to `limit` books in the same category,
 *                       excluding the current book (REQ-CAT-005).
 *
 * searchBooks()      — native PostgreSQL full-text search using
 *                       search_vector @@ to_tsquery() (CR-002).
 *                       Falls back to ILIKE for category/publisher names.
 *
 * searchBooksH2()    — simple ILIKE-style fallback used only in the H2
 *                       test environment where tsvector doesn't exist.
 *                       Activated by a Spring profile conditional
 *                       in BookService.
 *
 * existsByIsbn()     — used by BookLoader to skip already-loaded books.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Filtered, paginated book list.
     *
     * Each filter uses "IS NULL OR ..." so omitting a parameter has no effect:
     *   - :categoryId IS NULL  → ignore category filter
     *   - :publisherId IS NULL → ignore publisher filter
     *   - :minPrice IS NULL    → no lower price bound
     *   - :maxPrice IS NULL    → no upper price bound
     *   - :inStock = FALSE     → include all stock levels
     *     :inStock = TRUE      → only books with stockQuantity > 0
     *
     * JOIN FETCH ensures Publisher and Category are loaded in the same query
     * (avoids N+1 selects when mapping to DTOs).
     *
     * REQ-CAT-001, REQ-SRC-002
     */
    @Query(value = """
            SELECT b FROM Book b
            JOIN FETCH b.category c
            JOIN FETCH b.publisher p
            WHERE (:categoryId    IS NULL OR c.id    = :categoryId)
              AND (:publisherId   IS NULL OR p.id    = :publisherId)
              AND (:minPrice      IS NULL OR b.price >= :minPrice)
              AND (:maxPrice      IS NULL OR b.price <= :maxPrice)
              AND (:inStock       = FALSE OR b.stockQuantity > 0)
              AND (:categoryName  = '' OR LOWER(c.name) = LOWER(:categoryName))
              AND (:publisherName = '' OR LOWER(p.name) = LOWER(:publisherName))
            """,
            countQuery = """
            SELECT count(b) FROM Book b
            JOIN b.category c
            JOIN b.publisher p
            WHERE (:categoryId    IS NULL OR c.id    = :categoryId)
              AND (:publisherId   IS NULL OR p.id    = :publisherId)
              AND (:minPrice      IS NULL OR b.price >= :minPrice)
              AND (:maxPrice      IS NULL OR b.price <= :maxPrice)
              AND (:inStock       = FALSE OR b.stockQuantity > 0)
              AND (:categoryName  = '' OR LOWER(c.name) = LOWER(:categoryName))
              AND (:publisherName = '' OR LOWER(p.name) = LOWER(:publisherName))
            """)
    Page<Book> findFiltered(
            @Param("categoryId")    Long categoryId,
            @Param("publisherId")   Long publisherId,
            @Param("minPrice")      BigDecimal minPrice,
            @Param("maxPrice")      BigDecimal maxPrice,
            @Param("inStock")       boolean inStock,
            @Param("categoryName")  String categoryName,
            @Param("publisherName") String publisherName,
            Pageable pageable);

    /**
     * Related books: same category, different book, limited count.
     * Used by GET /api/books/{id}/related (REQ-CAT-005).
     *
     * JOIN FETCH publisher so it's available for DTO mapping.
     */
    @Query("""
            SELECT b FROM Book b
            JOIN FETCH b.category c
            JOIN FETCH b.publisher p
            WHERE c.id = :categoryId
              AND b.id <> :excludeId
            ORDER BY b.createdAt DESC
            """)
    List<Book> findRelated(
            @Param("categoryId") Long categoryId,
            @Param("excludeId")  Long excludeId,
            Pageable pageable);

    /**
     * Full-text search using PostgreSQL tsvector (CR-002).
     *
     * NATIVE QUERY — executed directly against PostgreSQL.
     * :tsQuery  — formatted tsquery string, e.g. "harry & potter"
     * :rawTerm  — raw search term for ILIKE fallback on category/publisher
     *
     * ts_rank sorts by relevance (title matches outrank author matches).
     * Results also include books whose category or publisher name
     * contains the search term (ILIKE is case-insensitive).
     *
     * REQ-SRC-001, CR-002
     *
     * NOTE: This query runs only against real PostgreSQL.
     * Tests use searchBooksH2() instead via BookService.search().
     */
    @Query(value = """
            SELECT b.*
            FROM books b
            JOIN categories c  ON b.category_id  = c.id
            JOIN publishers p  ON b.publisher_id = p.id,
                 to_tsquery('english', :tsQuery) query
            WHERE b.search_vector @@ query
               OR c.name ILIKE :rawTerm
               OR p.name ILIKE :rawTerm
            ORDER BY ts_rank(b.search_vector, query) DESC, b.created_at DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<Book> searchBooks(
            @Param("tsQuery")  String tsQuery,
            @Param("rawTerm")  String rawTerm,
            @Param("limit")    int limit);

    /**
     * Simple ILIKE search — used ONLY in H2 test environments.
     *
     * H2 does not support tsvector. This query uses JPQL LIKE
     * operators so Hibernate can translate it to any dialect.
     * It is called by BookService.search() when the active profile
     * is "test".
     *
     * REQ-SRC-001 (test coverage only — not production behaviour)
     */
    @Query("""
            SELECT b FROM Book b
            JOIN FETCH b.category c
            JOIN FETCH b.publisher p
            WHERE LOWER(b.title)   LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(b.authors) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(c.name)    LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(p.name)    LIKE LOWER(CONCAT('%', :term, '%'))
            ORDER BY b.createdAt DESC
            """)
    List<Book> searchBooksH2(@Param("term") String term, Pageable pageable);

    /**
     * Returns all books NOT in the given set of IDs, with category and publisher
     * eagerly fetched. Used by RecommendationService to get candidate books for
     * scoring. The set is always non-empty when this is called (guard in service).
     *
     * TASK-REC-001, REQ-REC-001
     */
    @Query("""
            SELECT b FROM Book b
            JOIN FETCH b.category
            JOIN FETCH b.publisher
            WHERE b.id NOT IN :excludedIds
            """)
    List<Book> findCandidatesExcluding(@Param("excludedIds") java.util.Set<Long> excludedIds);

    /**
     * Used by BookLoader to check if a book with the given ISBN
     * already exists — prevents duplicate insertions on every startup.
     */
    boolean existsByIsbn(String isbn);
}
