package com.ebookstore.backend.book;

import com.ebookstore.backend.book.dto.BookDetailDTO;
import com.ebookstore.backend.book.dto.BookFilterParams;
import com.ebookstore.backend.book.dto.BookSummaryDTO;
import com.ebookstore.backend.book.dto.PagedResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * BookController — handles all book catalogue HTTP endpoints.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-004 through TASK-CAT-007)
 * =============================================================
 * PUBLIC endpoints (no JWT required — SecurityConfig /api/books/**):
 *
 *   GET /api/books                — paginated, filtered catalogue (REQ-CAT-001, REQ-SRC-002)
 *   GET /api/books/{id}           — full book detail (REQ-CAT-004)
 *   GET /api/books/{id}/related   — related books strip (REQ-CAT-005)
 *   GET /api/books/search         — full-text search (REQ-SRC-001)
 *
 * All query parameters are optional — sensible defaults apply.
 * This controller is thin: it binds params, delegates to BookService,
 * and returns the HTTP response. No business logic lives here.
 *
 * PAGE RESPONSE SHAPE (GET /api/books):
 *   Wrapped in PagedResponseDTO to expose "page" field:
 *   {
 *     "content":       [...],
 *     "page":          0,
 *     "size":          20,
 *     "totalElements": 113,
 *     "totalPages":    6
 *   }
 *   (CR-005/BUG-002a — Spring's Page uses "number" not "page")
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ------------------------------------------------------------------
    // GET /api/books — Browse & Filter (TASK-CAT-004)
    // ------------------------------------------------------------------

    /**
     * Returns a paginated, optionally filtered list of books.
     *
     * All query parameters are optional. Examples:
     *   /api/books                                → first 20 books
     *   /api/books?category=Fiction&page=1        → category by name, page 2
     *   /api/books?publisher=Penguin              → publisher by name
     *   /api/books?minPrice=200&maxPrice=500      → price range
     *   /api/books?inStock=true&sort=price&dir=asc
     *
     * CR-005/BUG-002a: returns PagedResponseDTO (exposes "page" field)
     * CR-005/BUG-002b: accepts category/publisher by name string
     * REQ-CAT-001, REQ-SRC-002
     */
    @GetMapping
    public ResponseEntity<PagedResponseDTO<BookSummaryDTO>> browse(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "20")       int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc")     String dir) {

        BookFilterParams params = new BookFilterParams(
                category, publisher, categoryId, publisherId,
                minPrice, maxPrice, inStock, page, size, sort, dir);

        return ResponseEntity.ok(PagedResponseDTO.from(bookService.browse(params)));
    }

    // ------------------------------------------------------------------
    // GET /api/books/search — Full-text search (TASK-CAT-007)
    // ------------------------------------------------------------------

    /**
     * Searches books by title, author, publisher, or category name.
     *
     * Must be declared BEFORE /{id} so Spring doesn't try to parse
     * "search" as a Long path variable.
     *
     * Query params:
     *   q    — search term (required; returns empty list if blank)
     *   size — max results (default 20, max 50)
     *
     * REQ-SRC-001, CR-002
     */
    @GetMapping("/search")
    public ResponseEntity<List<BookSummaryDTO>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(bookService.search(q, size));
    }

    // ------------------------------------------------------------------
    // GET /api/books/{id} — Book Detail (TASK-CAT-005)
    // ------------------------------------------------------------------

    /**
     * Returns full details of a single book by ID.
     *
     * Includes tentative delivery date (today + 5 business days).
     * Returns 404 if the book ID does not exist.
     *
     * REQ-CAT-004
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookDetailDTO> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getDetail(id));
    }

    // ------------------------------------------------------------------
    // GET /api/books/{id}/related — Related Books (TASK-CAT-006)
    // ------------------------------------------------------------------

    /**
     * Returns up to 8 books in the same category as the given book.
     * Excludes the book itself.
     * Returns empty list if no related books exist.
     *
     * REQ-CAT-005
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<List<BookSummaryDTO>> getRelated(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getRelated(id));
    }
}
