package com.ebookstore.backend.cart;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import com.ebookstore.backend.book.Book;
import com.ebookstore.backend.book.BookRepository;
import com.ebookstore.backend.category.CategoryService;
import com.ebookstore.backend.publisher.PublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CartApiTest — TEST-CRT-001 through TEST-CRT-007
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M4, TASK-CRT-002, TASK-CRT-003, TASK-CRT-005)
 * =============================================================
 *
 * TEST-CRT-001  POST /api/cart/items adds book → GET /api/cart shows it
 * TEST-CRT-002  Adding same book twice → quantity = 2 (not two rows)
 * TEST-CRT-003  Adding out-of-stock book → 400
 * TEST-CRT-004  GET /api/cart returns correct subtotal, ₹40 delivery, grandTotal
 * TEST-CRT-005  DELETE /api/cart/items/{id} removes the item
 * TEST-CRT-006  POST /api/cart/merge — guest items appear in server cart
 * TEST-CRT-007  POST /api/cart/merge — duplicate book quantities are summed
 *
 * Uses H2 in-memory DB via @ActiveProfiles("test").
 * Each test registers a unique user so carts are isolated.
 */
@SpringBootTest
@ActiveProfiles("test")
class CartApiTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private BookRepository        bookRepository;
    @Autowired private CategoryService       categoryService;
    @Autowired private PublisherService      publisherService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Counter for generating unique user emails/phones per test. */
    private static final AtomicInteger userCounter = new AtomicInteger(900);

    private MockMvc mockMvc;

    // Book IDs set up once — shared across tests (books are not mutated)
    private Long inStockBookId;
    private Long outOfStockBookId;
    private Long inStockBook2Id;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create test books (idempotent via existsByIsbn)
        var cat = categoryService.findOrCreate("CartTest-Category");
        var pub = publisherService.findOrCreate("CartTest-Publisher");

        if (!bookRepository.existsByIsbn("9780099001001")) {
            Book b = new Book();
            b.setIsbn("9780099001001"); b.setTitle("CartTest Book In Stock");
            b.setAuthors("Author"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/1-M.jpg");
            b.setCategory(cat); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(500)); b.setStockQuantity(10);
            b.setLanguage("en");
            inStockBookId = bookRepository.save(b).getId();
        } else {
            inStockBookId = bookRepository.findAll().stream()
                    .filter(bk -> "9780099001001".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }

        if (!bookRepository.existsByIsbn("9780099001002")) {
            Book b = new Book();
            b.setIsbn("9780099001002"); b.setTitle("CartTest Book Out Of Stock");
            b.setAuthors("Author"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/2-M.jpg");
            b.setCategory(cat); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(300)); b.setStockQuantity(0);
            b.setLanguage("en");
            outOfStockBookId = bookRepository.save(b).getId();
        } else {
            outOfStockBookId = bookRepository.findAll().stream()
                    .filter(bk -> "9780099001002".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }

        if (!bookRepository.existsByIsbn("9780099001003")) {
            Book b = new Book();
            b.setIsbn("9780099001003"); b.setTitle("CartTest Book 2 In Stock");
            b.setAuthors("Author"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/3-M.jpg");
            b.setCategory(cat); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(200)); b.setStockQuantity(5);
            b.setLanguage("en");
            inStockBook2Id = bookRepository.save(b).getId();
        } else {
            inStockBook2Id = bookRepository.findAll().stream()
                    .filter(bk -> "9780099001003".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Registers a unique user and returns their JWT token. */
    private String registerAndGetToken() throws Exception {
        int n = userCounter.incrementAndGet();
        RegisterRequestDTO req = new RegisterRequestDTO(
                "Cart User " + n,
                "cartuser" + n + "@test.com",
                "80000" + String.format("%05d", n),
                "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponseDTO.class).token();
    }

    /** Performs POST /api/cart/items with the given bookId and quantity. */
    private MvcResult addToCart(String token, Long bookId, int qty) throws Exception {
        String body = """
                {"bookId": %d, "quantity": %d}
                """.formatted(bookId, qty);
        return mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    // ------------------------------------------------------------------

    /**
     * TEST-CRT-001
     * POST /api/cart/items adds a book → GET /api/cart shows it.
     * REQ-CRT-001 AC1, REQ-CRT-002 AC1
     */
    @Test
    @DisplayName("TEST-CRT-001: Add book to cart then GET /api/cart shows it")
    void addBookThenGetCartShowsIt() throws Exception {
        String token = registerAndGetToken();

        addToCart(token, inStockBookId, 1);

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].bookId").value(inStockBookId));
    }

    /**
     * TEST-CRT-002
     * Adding the same book twice → quantity = 2, not two rows.
     * REQ-CRT-001 AC2
     */
    @Test
    @DisplayName("TEST-CRT-002: Adding same book twice results in quantity = 2")
    void addSameBookTwiceResultsInQuantityTwo() throws Exception {
        String token = registerAndGetToken();

        addToCart(token, inStockBookId, 1);
        addToCart(token, inStockBookId, 1);

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    /**
     * TEST-CRT-003
     * Adding an out-of-stock book returns 400.
     * REQ-CRT-001 AC3
     */
    @Test
    @DisplayName("TEST-CRT-003: Adding out-of-stock book returns 400")
    void addOutOfStockBookReturns400() throws Exception {
        String token = registerAndGetToken();
        MvcResult result = addToCart(token, outOfStockBookId, 1);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    /**
     * TEST-CRT-004
     * GET /api/cart returns correct subtotal, ₹40 delivery charge, and grandTotal.
     * REQ-CRT-002 AC2
     */
    @Test
    @DisplayName("TEST-CRT-004: GET /api/cart returns correct totals")
    void getCartReturnsCorrectTotals() throws Exception {
        String token = registerAndGetToken();

        // Add in-stock book (₹500) qty 2 → itemTotal = ₹1000
        addToCart(token, inStockBookId, 2);

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // subtotal = 500 × 2 = 1000
                .andExpect(jsonPath("$.subtotal").value(1000.0))
                // deliveryCharge always ₹40
                .andExpect(jsonPath("$.deliveryCharge").value(40.0))
                // grandTotal = 1000 + 40 = 1040
                .andExpect(jsonPath("$.grandTotal").value(1040.0));
    }

    /**
     * TEST-CRT-004b
     * Empty cart returns items=[], deliveryCharge=40, grandTotal=40.
     * REQ-CRT-002 AC3
     */
    @Test
    @DisplayName("TEST-CRT-004b: Empty cart returns empty items and delivery charge only")
    void emptyCartReturnsDeliveryChargeOnly() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.subtotal").value(0.0))
                .andExpect(jsonPath("$.deliveryCharge").value(40.0));
    }

    /**
     * TEST-CRT-005
     * DELETE /api/cart/items/{id} removes the item from the cart.
     * REQ-CRT-003 AC2
     */
    @Test
    @DisplayName("TEST-CRT-005: DELETE /api/cart/items/{id} removes the item")
    void deleteItemRemovesItFromCart() throws Exception {
        String token = registerAndGetToken();

        // Add an item
        MvcResult addResult = addToCart(token, inStockBookId, 1);
        Long itemId = objectMapper.readTree(
                addResult.getResponse().getContentAsString()).get("id").asLong();

        // Delete it
        mockMvc.perform(delete("/api/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Cart should now be empty
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    /**
     * TEST-CRT-005b
     * PUT /api/cart/items/{id} updates the quantity.
     * REQ-CRT-003 AC1
     */
    @Test
    @DisplayName("TEST-CRT-005b: PUT /api/cart/items/{id} updates quantity")
    void updateItemQuantity() throws Exception {
        String token = registerAndGetToken();

        MvcResult addResult = addToCart(token, inStockBookId, 1);
        Long itemId = objectMapper.readTree(
                addResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));
    }

    /**
     * TEST-CRT-006
     * POST /api/cart/merge — guest cart items appear in server cart after merge.
     * REQ-CRT-005, REQ-USR-003
     */
    @Test
    @DisplayName("TEST-CRT-006: Merge guest cart items appear in server cart")
    void mergeGuestCartItemsAppearInServerCart() throws Exception {
        String token = registerAndGetToken();

        String mergeBody = """
                {
                  "items": [
                    {"bookId": %d, "quantity": 2},
                    {"bookId": %d, "quantity": 1}
                  ]
                }
                """.formatted(inStockBookId, inStockBook2Id);

        mockMvc.perform(post("/api/cart/merge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    /**
     * TEST-CRT-007
     * POST /api/cart/merge — duplicate book quantities are summed.
     * REQ-CRT-005 (merge logic: sum quantities on duplicate bookId)
     */
    @Test
    @DisplayName("TEST-CRT-007: Merge sums quantities for duplicate books")
    void mergeSumsQuantitiesForDuplicateBooks() throws Exception {
        String token = registerAndGetToken();

        // First add the book directly to server cart with qty 1
        addToCart(token, inStockBookId, 1);

        // Now merge with guest cart that also has the same book with qty 3
        String mergeBody = """
                {
                  "items": [
                    {"bookId": %d, "quantity": 3}
                  ]
                }
                """.formatted(inStockBookId);

        mockMvc.perform(post("/api/cart/merge")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody))
                .andExpect(status().isOk())
                // 1 (server) + 3 (guest) = 4
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    /**
     * TEST-CRT-008
     * GET /api/cart without JWT returns 401.
     * REQ-CRT-001 AC4
     */
    @Test
    @DisplayName("TEST-CRT-008: GET /api/cart without JWT returns 401")
    void getCartWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }
}
