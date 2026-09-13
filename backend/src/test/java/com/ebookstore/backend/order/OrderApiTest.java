package com.ebookstore.backend.order;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import com.ebookstore.backend.book.Book;
import com.ebookstore.backend.book.BookRepository;
import com.ebookstore.backend.category.CategoryService;
import com.ebookstore.backend.order.dto.CreateOrderDTO;
import com.ebookstore.backend.order.dto.OrderDetailDTO;
import com.ebookstore.backend.publisher.PublisherService;
import com.ebookstore.backend.user.Address;
import com.ebookstore.backend.user.AddressRepository;
import com.ebookstore.backend.user.User;
import com.ebookstore.backend.user.UserRepository;
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
 * OrderApiTest — TEST-ORD-001 through TEST-ORD-008
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M5, TASK-ORD-002–005)
 * =============================================================
 *
 * TEST-ORD-001  createOrder() clears cart and returns orderId
 * TEST-ORD-002  GET /api/orders returns user's orders most-recent first
 * TEST-ORD-003  GET /api/orders returns empty list (not 404) for new user
 * TEST-ORD-004  Buy Again adds items to cart, skips out-of-stock
 * TEST-ORD-005  Cancel CONFIRMED order → status = CANCELLED
 * TEST-ORD-006  Cancel DELIVERED order → 409
 * TEST-ORD-007  Cancel another user's order → 403
 * TEST-ORD-008  Gift points NOT restored after cancellation
 *
 * Strategy:
 *   createOrder() is an internal method called by PaymentService (M7).
 *   In M5 tests we invoke it directly via injected OrderService so we
 *   can test order behaviour without needing the Payment HTTP endpoint.
 *
 *   Each test registers a unique user via POST /api/auth/register to
 *   keep carts and order histories isolated.
 *
 *   Uses H2 in-memory DB via @ActiveProfiles("test").
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderApiTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private BookRepository        bookRepository;
    @Autowired private CategoryService       categoryService;
    @Autowired private PublisherService      publisherService;
    @Autowired private AddressRepository     addressRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private OrderService          orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Unique counter for user emails/phones — starts high to avoid M4 test collisions. */
    private static final AtomicInteger userCounter = new AtomicInteger(800);

    private MockMvc mockMvc;

    // Shared book ID — created once per test class run, reused across tests
    private Long inStockBookId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        var cat = categoryService.findOrCreate("OrderTest-Category");
        var pub = publisherService.findOrCreate("OrderTest-Publisher");

        if (!bookRepository.existsByIsbn("9780088001001")) {
            Book b = new Book();
            b.setIsbn("9780088001001"); b.setTitle("OrderTest Book In Stock");
            b.setAuthors("Author"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/10-M.jpg");
            b.setCategory(cat); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(400)); b.setStockQuantity(10);
            b.setLanguage("en");
            inStockBookId = bookRepository.save(b).getId();
        } else {
            inStockBookId = bookRepository.findAll().stream()
                    .filter(bk -> "9780088001001".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }

    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Registers a new unique user via HTTP, returns token + userId. */
    private AuthResponseDTO registerUser() throws Exception {
        int n = userCounter.incrementAndGet();
        RegisterRequestDTO req = new RegisterRequestDTO(
                "Order User " + n,
                "orderuser" + n + "@test.com",
                "90000" + String.format("%05d", n),
                "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponseDTO.class);
    }

    /** Saves a delivery address directly for the given user, returns addressId. */
    private Long createAddress(Long userId) {
        User user = userRepository.getReferenceById(userId);
        Address addr = new Address();
        addr.setUser(user);
        addr.setStreet("123 Test Street");
        addr.setCity("Mumbai");
        addr.setState("Maharashtra");
        addr.setPincode("400001");
        addr.setDefault(true);
        return addressRepository.save(addr).getId();
    }

    /** Adds a book to the user's cart via HTTP, returns itemId. */
    private void addToCart(String token, Long bookId, int qty) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d, \"quantity\": %d}".formatted(bookId, qty)))
                .andReturn();
    }

    /**
     * Calls OrderService.createOrder() directly (no Payment HTTP endpoint until M7).
     * Returns the full OrderDetailDTO.
     */
    private OrderDetailDTO placeOrder(Long userId, Long addressId, int giftPoints) {
        return orderService.createOrder(new CreateOrderDTO(userId, addressId, giftPoints));
    }

    // ------------------------------------------------------------------

    /**
     * TEST-ORD-001
     * createOrder() creates an order with status CONFIRMED and clears the cart.
     * REQ-ORD-001 AC1, AC2
     */
    @Test
    @DisplayName("TEST-ORD-001: createOrder clears cart and returns CONFIRMED order")
    void createOrderClearsCartAndReturnsConfirmedOrder() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        addToCart(auth.token(), inStockBookId, 2);

        OrderDetailDTO order = placeOrder(auth.userId(), addressId, 0);

        assertThat(order.id()).isNotNull();
        assertThat(order.status()).isEqualTo("CONFIRMED");
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).quantity()).isEqualTo(2);

        // Cart must be empty after order (REQ-ORD-001 AC2)
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    /**
     * TEST-ORD-002
     * GET /api/orders returns the user's orders, most recent first.
     * REQ-ORD-002 AC1
     */
    @Test
    @DisplayName("TEST-ORD-002: GET /api/orders returns user orders most-recent first")
    void listOrdersReturnsMostRecentFirst() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        // Place two orders
        addToCart(auth.token(), inStockBookId, 1);
        OrderDetailDTO first = placeOrder(auth.userId(), addressId, 0);

        addToCart(auth.token(), inStockBookId, 1);
        OrderDetailDTO second = placeOrder(auth.userId(), addressId, 0);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Most recent (second) should be first in the list
                .andExpect(jsonPath("$[0].id").value(second.id()))
                .andExpect(jsonPath("$[1].id").value(first.id()));
    }

    /**
     * TEST-ORD-003
     * GET /api/orders returns empty list for a new user — not 404.
     * REQ-ORD-002 AC3
     */
    @Test
    @DisplayName("TEST-ORD-003: GET /api/orders returns empty list for new user")
    void listOrdersReturnsEmptyListForNewUser() throws Exception {
        AuthResponseDTO auth = registerUser();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * TEST-ORD-004
     * POST /api/orders/{id}/buy-again adds in-stock items to cart,
     * skips out-of-stock items and reports them in skippedItems.
     * REQ-ORD-003 AC1, AC2
     */
    @Test
    @DisplayName("TEST-ORD-004: Buy Again adds in-stock items, skips out-of-stock")
    void buyAgainAddsInStockSkipsOutOfStock() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        // Place order with one in-stock book
        addToCart(auth.token(), inStockBookId, 1);
        OrderDetailDTO order = placeOrder(auth.userId(), addressId, 0);

        // Make the book out of stock by setting stockQuantity = 0
        Book book = bookRepository.findById(inStockBookId).orElseThrow();
        book.setStockQuantity(0);
        bookRepository.save(book);

        mockMvc.perform(post("/api/orders/" + order.id() + "/buy-again")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addedItems.length()").value(0))
                .andExpect(jsonPath("$.skippedItems.length()").value(1))
                .andExpect(jsonPath("$.skippedItems[0].reason").value("OUT_OF_STOCK"));

        // Restore stock for other tests
        book.setStockQuantity(10);
        bookRepository.save(book);
    }

    /**
     * TEST-ORD-005
     * POST /api/orders/{id}/cancel sets status to CANCELLED for a CONFIRMED order.
     * REQ-ORD-004 AC1
     */
    @Test
    @DisplayName("TEST-ORD-005: Cancel CONFIRMED order sets status to CANCELLED")
    void cancelConfirmedOrderSetsStatusCancelled() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        addToCart(auth.token(), inStockBookId, 1);
        OrderDetailDTO order = placeOrder(auth.userId(), addressId, 0);
        assertThat(order.status()).isEqualTo("CONFIRMED");

        mockMvc.perform(post("/api/orders/" + order.id() + "/cancel")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    /**
     * TEST-ORD-006
     * POST /api/orders/{id}/cancel on a DELIVERED order → 409 Conflict.
     * REQ-ORD-004 AC2
     */
    @Test
    @DisplayName("TEST-ORD-006: Cancel DELIVERED order returns 409")
    void cancelDeliveredOrderReturns409() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        addToCart(auth.token(), inStockBookId, 1);
        OrderDetailDTO order = placeOrder(auth.userId(), addressId, 0);

        // Force status to DELIVERED directly via repository (no HTTP endpoint for status changes)
        com.ebookstore.backend.order.Order raw = getOrderRepository().findById(order.id()).orElseThrow();
        raw.setStatus(OrderStatus.DELIVERED);
        getOrderRepository().save(raw);

        mockMvc.perform(post("/api/orders/" + order.id() + "/cancel")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isConflict());
    }

    /**
     * TEST-ORD-007
     * POST /api/orders/{id}/cancel with a different user's token → 403.
     * REQ-ORD-004 AC3
     */
    @Test
    @DisplayName("TEST-ORD-007: Cancel another user's order returns 403")
    void cancelAnotherUsersOrderReturns403() throws Exception {
        AuthResponseDTO owner   = registerUser();
        AuthResponseDTO other   = registerUser();
        Long addressId = createAddress(owner.userId());

        addToCart(owner.token(), inStockBookId, 1);
        OrderDetailDTO order = placeOrder(owner.userId(), addressId, 0);

        mockMvc.perform(post("/api/orders/" + order.id() + "/cancel")
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isForbidden());
    }

    /**
     * TEST-ORD-008
     * Gift points are NOT restored after cancelling an order.
     * REQ-ORD-004 AC4, Decision D-010
     */
    @Test
    @DisplayName("TEST-ORD-008: Gift points not restored after cancellation")
    void giftPointsNotRestoredAfterCancellation() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        // Give user 10 gift points manually
        User user = userRepository.findById(auth.userId()).orElseThrow();
        user.setGiftPointBalance(10);
        userRepository.save(user);

        // Place order redeeming 5 points → grandTotal = (400 - 10) + 40 = 430
        addToCart(auth.token(), inStockBookId, 1);
        OrderDetailDTO order = placeOrder(auth.userId(), addressId, 5);
        assertThat(order.giftPointsRedeemed()).isEqualTo(5);

        // Verify balance after order: 10 - 5 = 5 (redeemed), + floor(430/50) = 8 earned → 13
        User afterOrder = userRepository.findById(auth.userId()).orElseThrow();
        int balanceAfterOrder = afterOrder.getGiftPointBalance();

        // Cancel the order
        mockMvc.perform(post("/api/orders/" + order.id() + "/cancel")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());

        // Balance must NOT change — redeemed points are forfeited (D-010)
        User afterCancel = userRepository.findById(auth.userId()).orElseThrow();
        assertThat(afterCancel.getGiftPointBalance()).isEqualTo(balanceAfterOrder);
    }

    // ------------------------------------------------------------------
    // Private — expose OrderRepository for forced status override in TEST-ORD-006
    // ------------------------------------------------------------------

    @Autowired
    private OrderRepository orderRepository;

    private OrderRepository getOrderRepository() {
        return orderRepository;
    }
}
