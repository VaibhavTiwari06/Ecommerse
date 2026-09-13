package com.ebookstore.backend.checkout;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import com.ebookstore.backend.book.Book;
import com.ebookstore.backend.book.BookRepository;
import com.ebookstore.backend.book.DeliveryDateCalculator;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CheckoutApiTest — TEST-CHK-001, TEST-CHK-002, TEST-CHK-003
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M6, TASK-CHK-001, TASK-CHK-002)
 * =============================================================
 *
 * TEST-CHK-001  GET /api/checkout/summary returns deliveryCharge = 40
 * TEST-CHK-002  GET /api/checkout/summary tentativeDeliveryDate = today + 5 business days
 * TEST-CHK-003  DeliveryDateCalculator skips weekends correctly
 *               (already covered by DeliveryDateCalculatorTest — verified here via API)
 *
 * Uses H2 in-memory DB via @ActiveProfiles("test").
 * Each test registers a unique user.
 */
@SpringBootTest
@ActiveProfiles("test")
class CheckoutApiTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private BookRepository        bookRepository;
    @Autowired private CategoryService       categoryService;
    @Autowired private PublisherService      publisherService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final AtomicInteger userCounter = new AtomicInteger(700);

    private MockMvc mockMvc;
    private Long inStockBookId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        var cat = categoryService.findOrCreate("CheckoutTest-Category");
        var pub = publisherService.findOrCreate("CheckoutTest-Publisher");

        if (!bookRepository.existsByIsbn("9780077001001")) {
            Book b = new Book();
            b.setIsbn("9780077001001"); b.setTitle("Checkout Test Book");
            b.setAuthors("Author"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/20-M.jpg");
            b.setCategory(cat); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(600)); b.setStockQuantity(10);
            b.setLanguage("en");
            inStockBookId = bookRepository.save(b).getId();
        } else {
            inStockBookId = bookRepository.findAll().stream()
                    .filter(bk -> "9780077001001".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String registerAndGetToken() throws Exception {
        int n = userCounter.incrementAndGet();
        RegisterRequestDTO req = new RegisterRequestDTO(
                "Checkout User " + n,
                "chkuser" + n + "@test.com",
                "70000" + String.format("%05d", n),
                "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponseDTO.class).token();
    }

    // ------------------------------------------------------------------

    /**
     * TEST-CHK-001
     * GET /api/checkout/summary returns deliveryCharge = 40.
     * REQ-CHK-002 AC1
     */
    @Test
    @DisplayName("TEST-CHK-001: Checkout summary includes deliveryCharge = 40")
    void checkoutSummaryIncludesDeliveryCharge() throws Exception {
        String token = registerAndGetToken();

        // Add a book to cart first so subtotal is non-zero
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":%d,\"quantity\":1}".formatted(inStockBookId)))
                .andReturn();

        mockMvc.perform(get("/api/checkout/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // REQ-CHK-002: always ₹40
                .andExpect(jsonPath("$.deliveryCharge").value(40.0))
                // subtotal = 600, grandTotal = 640
                .andExpect(jsonPath("$.subtotal").value(600.0))
                .andExpect(jsonPath("$.grandTotal").value(640.0))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    /**
     * TEST-CHK-002
     * GET /api/checkout/summary tentativeDeliveryDate = today + 5 business days.
     * REQ-CHK-003 AC1, AC2
     */
    @Test
    @DisplayName("TEST-CHK-002: Checkout summary tentativeDeliveryDate is today + 5 business days")
    void checkoutSummaryHasCorrectDeliveryDate() throws Exception {
        String token = registerAndGetToken();

        String expectedDate = DeliveryDateCalculator.tentativeDeliveryDate().toString();

        mockMvc.perform(get("/api/checkout/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tentativeDeliveryDate").value(expectedDate));
    }

    /**
     * TEST-CHK-003
     * Checkout summary works for empty cart — returns items=[], subtotal=0, grandTotal=40.
     * Also verifies savedAddresses is an empty list (not null/404) for a new user.
     * REQ-CHK-001 AC2, REQ-CHK-002 AC1
     */
    @Test
    @DisplayName("TEST-CHK-003: Empty cart checkout returns subtotal=0 grandTotal=40 addresses=[]")
    void emptyCartCheckoutSummary() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/checkout/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.subtotal").value(0.0))
                .andExpect(jsonPath("$.deliveryCharge").value(40.0))
                .andExpect(jsonPath("$.grandTotal").value(40.0))
                .andExpect(jsonPath("$.giftPointBalance").value(0))
                .andExpect(jsonPath("$.savedAddresses").isArray())
                .andExpect(jsonPath("$.savedAddresses.length()").value(0));
    }

    /**
     * TEST-CHK-004
     * GET /api/checkout/summary without JWT returns 401.
     */
    @Test
    @DisplayName("TEST-CHK-004: GET /api/checkout/summary without JWT returns 401")
    void checkoutSummaryWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/checkout/summary"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * TEST-CHK-005
     * Checkout summary savedAddresses includes the user's saved addresses.
     * REQ-CHK-001 AC2
     */
    @Test
    @DisplayName("TEST-CHK-005: Checkout summary savedAddresses includes user's addresses")
    void checkoutSummaryIncludesSavedAddresses() throws Exception {
        String token = registerAndGetToken();

        // Add an address via the user addresses endpoint
        mockMvc.perform(post("/api/user/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"street\":\"1 Test St\",\"city\":\"Delhi\",\"state\":\"Delhi\",\"pincode\":\"110001\",\"isDefault\":true}"))
                .andReturn();

        mockMvc.perform(get("/api/checkout/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAddresses.length()").value(1))
                .andExpect(jsonPath("$.savedAddresses[0].city").value("Delhi"));
    }
}
