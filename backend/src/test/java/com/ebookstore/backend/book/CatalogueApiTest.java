package com.ebookstore.backend.book;

import com.ebookstore.backend.category.Category;
import com.ebookstore.backend.category.CategoryService;
import com.ebookstore.backend.publisher.Publisher;
import com.ebookstore.backend.publisher.PublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CatalogueApiTest — TEST-CAT-002
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M3, TASK-CAT-004 through TASK-CAT-007)
 * =============================================================
 * Full HTTP integration tests through the Spring MVC + Security filter chain.
 *
 * TEST-CAT-002a  GET /api/books         returns 200 + page structure
 * TEST-CAT-002b  GET /api/books?categoryId=N  filters correctly
 * TEST-CAT-002c  GET /api/books?inStock=true  filters out-of-stock books
 * TEST-CAT-002d  GET /api/books/{id}    returns 200 + book fields
 * TEST-CAT-002e  GET /api/books/999999  returns 404
 * TEST-CAT-002f  GET /api/books/{id}/related   returns 200 + list
 * TEST-CAT-002g  GET /api/books/search?q=X      returns 200 + list
 * TEST-CAT-002h  GET /api/books/search?q=       returns 200 + empty list
 * TEST-CAT-002i  GET /api/books  is accessible without JWT (public endpoint)
 * TEST-CAT-002j  GET /api/categories  returns list with at least one entry
 * TEST-CAT-002k  GET /api/publishers  returns list with at least one entry
 *
 * Uses H2 in-memory database. Test data is inserted once in @BeforeEach
 * and wrapped in a transaction that rolls back after each test.
 *
 * NOTE: @Transactional on the test class rolls back after each test method.
 * However, MockMvc tests run outside the test transaction by default — so
 * we insert data outside @Transactional and rely on unique ISBN prefixes
 * to avoid conflicts. Each @BeforeEach re-uses findOrCreate() which is
 * idempotent.
 */
@SpringBootTest
@ActiveProfiles("test")
class CatalogueApiTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private BookRepository     bookRepository;
    @Autowired private CategoryService    categoryService;
    @Autowired private PublisherService   publisherService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    // IDs of books inserted for these tests — set in @BeforeEach
    private Long bookFictionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create test data (idempotent — findOrCreate for categories/publishers)
        Category fiction  = categoryService.findOrCreate("CatalogueTest-Fiction");
        Category science  = categoryService.findOrCreate("CatalogueTest-Science");
        Publisher pubA    = publisherService.findOrCreate("CatalogueTest-PubA");

        // Insert fiction book (in stock)
        if (!bookRepository.existsByIsbn("9780099000001")) {
            Book b = new Book();
            b.setIsbn("9780099000001");
            b.setTitle("CatalogueTest Fiction Book");
            b.setAuthors("Test Author Alpha");
            b.setDescription("A test fiction book for CatalogueApiTest.");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/1-M.jpg");
            b.setCategory(fiction);
            b.setPublisher(pubA);
            b.setPrice(BigDecimal.valueOf(299));
            b.setStockQuantity(10);
            b.setLanguage("en");
            bookFictionId = bookRepository.save(b).getId();
        } else {
            bookFictionId = bookRepository.findAll().stream()
                    .filter(bk -> "9780099000001".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }

        // Insert science book (in stock)
        if (!bookRepository.existsByIsbn("9780099000002")) {
            Book b = new Book();
            b.setIsbn("9780099000002");
            b.setTitle("CatalogueTest Science Book");
            b.setAuthors("Test Author Beta");
            b.setDescription("A test science book for CatalogueApiTest.");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/2-M.jpg");
            b.setCategory(science);
            b.setPublisher(pubA);
            b.setPrice(BigDecimal.valueOf(499));
            b.setStockQuantity(5);
            b.setLanguage("en");
            bookRepository.save(b);
        }

        // Insert out-of-stock book
        if (!bookRepository.existsByIsbn("9780099000003")) {
            Book b = new Book();
            b.setIsbn("9780099000003");
            b.setTitle("CatalogueTest Out Of Stock Book");
            b.setAuthors("Test Author Gamma");
            b.setDescription("A test out-of-stock book.");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/3-M.jpg");
            b.setCategory(fiction);
            b.setPublisher(pubA);
            b.setPrice(BigDecimal.valueOf(150));
            b.setStockQuantity(0);
            b.setLanguage("en");
            bookRepository.save(b);
        }
    }

    // ------------------------------------------------------------------

    /**
     * TEST-CAT-002a
     * GET /api/books returns 200 with a paged response containing books.
     * REQ-CAT-001
     */
    @Test
    @DisplayName("TEST-CAT-002a: GET /api/books returns 200 with page structure")
    void getBooksReturns200WithPageStructure() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    /**
     * TEST-CAT-002b
     * GET /api/books?categoryId=N returns only books in that category.
     * REQ-SRC-002
     */
    @Test
    @DisplayName("TEST-CAT-002b: GET /api/books?categoryId filters correctly")
    void getBooksFiltersByCategoryId() throws Exception {
        // Get category ID of the fiction category
        Category fiction = categoryService.findOrCreate("CatalogueTest-Fiction");

        mockMvc.perform(get("/api/books").param("categoryId", fiction.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].category").value("CatalogueTest-Fiction"));
    }

    /**
     * TEST-CAT-002c
     * GET /api/books?inStock=true excludes out-of-stock books.
     * REQ-CAT-006, REQ-SRC-002
     */
    @Test
    @DisplayName("TEST-CAT-002c: GET /api/books?inStock=true excludes out-of-stock books")
    void getBooksInStockExcludesOutOfStock() throws Exception {
        String response = mockMvc.perform(get("/api/books").param("inStock", "true"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Parse content array and verify all have inStock=true
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
        com.fasterxml.jackson.databind.JsonNode content = root.get("content");
        assertThat(content.isArray()).isTrue();
        for (com.fasterxml.jackson.databind.JsonNode book : content) {
            assertThat(book.get("inStock").asBoolean())
                    .as("Book '%s' should be in stock", book.get("title").asText())
                    .isTrue();
        }
    }

    /**
     * TEST-CAT-002d
     * GET /api/books/{id} returns 200 with correct book fields.
     * REQ-CAT-004
     */
    @Test
    @DisplayName("TEST-CAT-002d: GET /api/books/{id} returns 200 with book detail")
    void getBookDetailReturns200() throws Exception {
        mockMvc.perform(get("/api/books/" + bookFictionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookFictionId))
                .andExpect(jsonPath("$.title").value("CatalogueTest Fiction Book"))
                .andExpect(jsonPath("$.authors").value("Test Author Alpha"))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.tentativeDeliveryDate").isNotEmpty());
    }

    /**
     * TEST-CAT-002e
     * GET /api/books/999999 returns 404 for a non-existent book.
     * REQ-CAT-004
     */
    @Test
    @DisplayName("TEST-CAT-002e: GET /api/books/999999 returns 404")
    void getBookDetailReturns404ForMissingBook() throws Exception {
        mockMvc.perform(get("/api/books/999999"))
                .andExpect(status().isNotFound());
    }

    /**
     * TEST-CAT-002f
     * GET /api/books/{id}/related returns 200 with a list (may be empty).
     * REQ-CAT-005
     */
    @Test
    @DisplayName("TEST-CAT-002f: GET /api/books/{id}/related returns 200 with list")
    void getRelatedBooksReturns200() throws Exception {
        mockMvc.perform(get("/api/books/" + bookFictionId + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * TEST-CAT-002g
     * GET /api/books/search?q=CatalogueTest returns 200 with matching results.
     * REQ-SRC-001
     */
    @Test
    @DisplayName("TEST-CAT-002g: GET /api/books/search?q=CatalogueTest returns matches")
    void searchBooksReturnsMatches() throws Exception {
        mockMvc.perform(get("/api/books/search").param("q", "CatalogueTest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    /**
     * TEST-CAT-002h
     * GET /api/books/search?q= (blank term) returns 200 with empty list.
     * REQ-SRC-001
     */
    @Test
    @DisplayName("TEST-CAT-002h: GET /api/books/search?q= returns empty list")
    void searchWithBlankTermReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/books/search").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * TEST-CAT-002i
     * GET /api/books is accessible without a JWT (public endpoint).
     * REQ-USR-001, design §3.4
     */
    @Test
    @DisplayName("TEST-CAT-002i: GET /api/books without JWT returns 200 (public endpoint)")
    void getBooksIsPublicWithoutJwt() throws Exception {
        // No Authorization header — should still return 200
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());
    }

    /**
     * TEST-CAT-002j
     * GET /api/categories returns 200 with at least one category.
     * REQ-CAT-002
     */
    @Test
    @DisplayName("TEST-CAT-002j: GET /api/categories returns 200 with list")
    void getCategoriesReturns200() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    /**
     * TEST-CAT-002k
     * GET /api/publishers returns 200 with at least one publisher.
     * REQ-CAT-003
     */
    @Test
    @DisplayName("TEST-CAT-002k: GET /api/publishers returns 200 with list")
    void getPublishersReturns200() throws Exception {
        mockMvc.perform(get("/api/publishers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
