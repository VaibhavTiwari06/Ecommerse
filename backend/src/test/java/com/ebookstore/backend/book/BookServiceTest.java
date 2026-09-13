package com.ebookstore.backend.book;

import com.ebookstore.backend.book.dto.BookSummaryDTO;
import com.ebookstore.backend.book.dto.BookDetailDTO;
import com.ebookstore.backend.book.dto.BookFilterParams;
import com.ebookstore.backend.category.Category;
import com.ebookstore.backend.category.CategoryService;
import com.ebookstore.backend.publisher.Publisher;
import com.ebookstore.backend.publisher.PublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BookServiceTest — TEST-CAT-001
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M3, TASK-CAT-004 through TASK-CAT-007)
 * =============================================================
 * Unit-level tests for BookService using the real Spring context
 * with H2 in-memory database.
 *
 * Pre-conditions:
 *   Each test that needs books inserts them directly via repositories
 *   rather than relying on BookLoader (seed file path is not in test
 *   classpath in the same location — tests are self-contained).
 *
 * TEST-CAT-001a  browse() returns paginated results
 * TEST-CAT-001b  browse() with categoryId filter returns only that category
 * TEST-CAT-001c  browse() with inStock=true returns only in-stock books
 * TEST-CAT-001d  browse() with price range filter
 * TEST-CAT-001e  getDetail() returns correct fields including delivery date
 * TEST-CAT-001f  getDetail() throws EntityNotFoundException for missing id
 * TEST-CAT-001g  getRelated() returns books in same category, excluding self
 * TEST-CAT-001h  search() returns matching books
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookServiceTest {

    @Autowired private BookService bookService;
    @Autowired private BookRepository bookRepository;
    @Autowired private CategoryService categoryService;
    @Autowired private PublisherService publisherService;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Book makeBook(String isbn, String title, String authors,
                          Category cat, Publisher pub,
                          double price, int stock) {
        Book b = new Book();
        b.setIsbn(isbn);
        b.setTitle(title);
        b.setAuthors(authors);
        b.setDescription("A test book description.");
        b.setCoverImageUrl("https://covers.openlibrary.org/b/id/1-M.jpg");
        b.setCategory(cat);
        b.setPublisher(pub);
        b.setPrice(BigDecimal.valueOf(price));
        b.setStockQuantity(stock);
        b.setLanguage("en");
        return bookRepository.save(b);
    }

    // ------------------------------------------------------------------

    /**
     * TEST-CAT-001a
     * browse() with no filters returns a Page of results.
     * REQ-CAT-001
     */
    @Test
    @DisplayName("TEST-CAT-001a: browse() returns paginated book list")
    void browseReturnsPaginatedList() {
        Category cat = categoryService.findOrCreate("Test Fiction");
        Publisher pub = publisherService.findOrCreate("Test Publisher");
        makeBook("9780000000001", "Book Alpha", "Author A", cat, pub, 299.0, 5);
        makeBook("9780000000002", "Book Beta",  "Author B", cat, pub, 399.0, 3);
        makeBook("9780000000003", "Book Gamma", "Author C", cat, pub, 499.0, 0);

        BookFilterParams params = new BookFilterParams(null, null, null, null, null, null,
                null, 0, 10, "createdAt", "desc");
        Page<BookSummaryDTO> page = bookService.browse(params);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    /**
     * TEST-CAT-001b
     * browse() with categoryId filter returns only books in that category.
     * REQ-SRC-002
     */
    @Test
    @DisplayName("TEST-CAT-001b: browse() with categoryId returns only that category")
    void browseFiltersByCategoryId() {
        Category catA = categoryService.findOrCreate("Sci-Fi");
        Category catB = categoryService.findOrCreate("History");
        Publisher pub  = publisherService.findOrCreate("Pub B");

        makeBook("9780000000010", "Sci-Fi Book", "Author X", catA, pub, 199.0, 2);
        makeBook("9780000000011", "History Book","Author Y", catB, pub, 250.0, 1);

        BookFilterParams params = new BookFilterParams(null, null, catA.getId(), null, null, null,
                null, 0, 20, "title", "asc");
        Page<BookSummaryDTO> page = bookService.browse(params);

        assertThat(page.getContent())
                .isNotEmpty()
                .allMatch(b -> b.categoryName().equals("Sci-Fi"));
    }

    /**
     * TEST-CAT-001c
     * browse() with inStock=true excludes out-of-stock books.
     * REQ-SRC-002, REQ-CAT-006
     */
    @Test
    @DisplayName("TEST-CAT-001c: browse() with inStock=true excludes out-of-stock books")
    void browseFiltersInStockOnly() {
        Category cat = categoryService.findOrCreate("Mystery");
        Publisher pub = publisherService.findOrCreate("Mystery Press");
        makeBook("9780000000020", "In Stock Book",  "A", cat, pub, 300.0, 5);
        makeBook("9780000000021", "Out Stock Book", "B", cat, pub, 300.0, 0);

        BookFilterParams params = new BookFilterParams(null, null, cat.getId(), null, null, null,
                true, 0, 20, "title", "asc");
        Page<BookSummaryDTO> page = bookService.browse(params);

        assertThat(page.getContent())
                .isNotEmpty()
                .allMatch(BookSummaryDTO::inStock);
    }

    /**
     * TEST-CAT-001d
     * browse() with minPrice and maxPrice filter.
     * REQ-SRC-002
     */
    @Test
    @DisplayName("TEST-CAT-001d: browse() filters by price range")
    void browseFiltersByPriceRange() {
        Category cat = categoryService.findOrCreate("Thriller");
        Publisher pub = publisherService.findOrCreate("Thriller Press");
        makeBook("9780000000030", "Cheap Book",     "A", cat, pub, 100.0, 5);
        makeBook("9780000000031", "Mid Price Book", "B", cat, pub, 300.0, 5);
        makeBook("9780000000032", "Expensive Book", "C", cat, pub, 900.0, 5);

        BookFilterParams params = new BookFilterParams(null, null, null, null,
                BigDecimal.valueOf(200), BigDecimal.valueOf(500),
                null, 0, 20, "price", "asc");
        Page<BookSummaryDTO> page = bookService.browse(params);

        assertThat(page.getContent())
                .isNotEmpty()
                .allMatch(b -> b.price().compareTo(BigDecimal.valueOf(200)) >= 0
                        && b.price().compareTo(BigDecimal.valueOf(500)) <= 0);
    }

    /**
     * TEST-CAT-001e
     * getDetail() returns all fields and a delivery date in the future.
     * REQ-CAT-004
     */
    @Test
    @DisplayName("TEST-CAT-001e: getDetail() returns correct fields + future delivery date")
    void getDetailReturnsCorrectFields() {
        Category cat = categoryService.findOrCreate("Biography");
        Publisher pub = publisherService.findOrCreate("Bio Press");
        Book saved = makeBook("9780000000040", "Life Story", "Famous Person",
                cat, pub, 450.0, 8);

        BookDetailDTO detail = bookService.getDetail(saved.getId());

        assertThat(detail.id()).isEqualTo(saved.getId());
        assertThat(detail.title()).isEqualTo("Life Story");
        assertThat(detail.authors()).isEqualTo("Famous Person");
        assertThat(detail.categoryName()).isEqualTo("Biography");
        assertThat(detail.publisherName()).isEqualTo("Bio Press");
        assertThat(detail.price()).isEqualByComparingTo(BigDecimal.valueOf(450.0));
        assertThat(detail.inStock()).isTrue();
        assertThat(detail.stockQuantity()).isEqualTo(8);
        // Delivery date must be in the future
        assertThat(detail.tentativeDeliveryDate()).isAfter(java.time.LocalDate.now());
    }

    /**
     * TEST-CAT-001f
     * getDetail() throws EntityNotFoundException for a non-existent book ID.
     * REQ-CAT-004
     */
    @Test
    @DisplayName("TEST-CAT-001f: getDetail() throws EntityNotFoundException for missing book")
    void getDetailThrowsForMissingBook() {
        org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.persistence.EntityNotFoundException.class,
                () -> bookService.getDetail(999999L)
        );
    }

    /**
     * TEST-CAT-001g
     * getRelated() returns books in the same category excluding the given book.
     * REQ-CAT-005
     */
    @Test
    @DisplayName("TEST-CAT-001g: getRelated() returns same-category books excluding self")
    void getRelatedReturnsSameCategoryExcludingSelf() {
        Category cat = categoryService.findOrCreate("Romance");
        Publisher pub = publisherService.findOrCreate("Romance Press");
        Book book1 = makeBook("9780000000050", "Love Story 1", "A", cat, pub, 200.0, 3);
        Book book2 = makeBook("9780000000051", "Love Story 2", "B", cat, pub, 250.0, 2);
        Book book3 = makeBook("9780000000052", "Love Story 3", "C", cat, pub, 300.0, 1);

        List<BookSummaryDTO> related = bookService.getRelated(book1.getId());

        // Should not contain book1 itself
        assertThat(related).noneMatch(b -> b.id().equals(book1.getId()));
        // Should contain book2 and book3
        assertThat(related).anyMatch(b -> b.id().equals(book2.getId()));
        assertThat(related).anyMatch(b -> b.id().equals(book3.getId()));
    }

    /**
     * TEST-CAT-001h
     * search() returns books matching the term (H2 ILIKE fallback in test profile).
     * REQ-SRC-001
     */
    @Test
    @DisplayName("TEST-CAT-001h: search() returns books matching the term")
    void searchReturnsMatchingBooks() {
        Category cat = categoryService.findOrCreate("Science");
        Publisher pub = publisherService.findOrCreate("Science Press");
        makeBook("9780000000060", "Quantum Physics Guide", "Richard Feynman", cat, pub, 599.0, 4);
        makeBook("9780000000061", "Organic Chemistry",     "Linus Pauling",   cat, pub, 499.0, 6);

        List<BookSummaryDTO> results = bookService.search("Quantum", 10);

        assertThat(results)
                .isNotEmpty()
                .anyMatch(b -> b.title().contains("Quantum"));
    }

    /**
     * TEST-CAT-001i
     * search() with blank term returns empty list.
     * REQ-SRC-001
     */
    @Test
    @DisplayName("TEST-CAT-001i: search() with blank term returns empty list")
    void searchWithBlankTermReturnsEmpty() {
        List<BookSummaryDTO> results = bookService.search("  ", 10);
        assertThat(results).isEmpty();
    }
}
