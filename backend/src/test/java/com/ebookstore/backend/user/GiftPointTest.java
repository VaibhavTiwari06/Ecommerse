package com.ebookstore.backend.user;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import com.ebookstore.backend.book.Book;
import com.ebookstore.backend.book.BookRepository;
import com.ebookstore.backend.category.CategoryService;
import com.ebookstore.backend.order.OrderService;
import com.ebookstore.backend.order.dto.CreateOrderDTO;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GiftPointTest — TEST-GFT-001 through TEST-GFT-006
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M8, TASK-GFT-001–003)
 * =============================================================
 *
 * TEST-GFT-001  Order of ₹500 awards floor(500/50) = 10 gift points
 * TEST-GFT-002  Order of ₹149 awards floor(149/50) = 2 gift points
 * TEST-GFT-003  Redeeming 5 points gives ₹10 discount on grand total
 * TEST-GFT-004  Redeeming more points than balance → 400
 * TEST-GFT-005  GET /api/user/giftpoints returns correct balance
 * TEST-GFT-006  Cancelled order does NOT restore redeemed points
 *
 * Strategy: createOrder() is called directly via injected OrderService
 * (no Payment HTTP endpoint until M7). This is the same approach used
 * in OrderApiTest.
 *
 * Uses H2 in-memory DB via @ActiveProfiles("test").
 * Each test registers a unique user so balances are isolated.
 */
@SpringBootTest
@ActiveProfiles("test")
class GiftPointTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private BookRepository        bookRepository;
    @Autowired private CategoryService       categoryService;
    @Autowired private PublisherService      publisherService;
    @Autowired private UserRepository        userRepository;
    @Autowired private AddressRepository     addressRepository;
    @Autowired private OrderService          orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicInteger userCounter = new AtomicInteger(600);

    private MockMvc mockMvc;

    // Books set up once — their prices drive the gift point maths
    private Long book500Id;   // price = ₹500 → order grand = ₹540 → earns floor(540/50)=10 pts
    private Long book149Id;   // price = ₹149 → order grand = ₹189 → earns floor(189/50)=3 pts
                              // NOTE: grandTotal includes ₹40 delivery; spec says floor(grandTotal/50)

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        var cat = categoryService.findOrCreate("GiftTest-Category");
        var pub = publisherService.findOrCreate("GiftTest-Publisher");

        if (!bookRepository.existsByIsbn("9780066001001")) {
            Book b = new Book();
            b.setIsbn("9780066001001"); b.setTitle("GiftTest Book 500");
            b.setAuthors("Author"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/30-M.jpg");
            b.setCategory(cat); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(500)); b.setStockQuantity(50);
            b.setLanguage("en");
            book500Id = bookRepository.save(b).getId();
        } else {
            book500Id = bookRepository.findAll().stream()
                    .filter(bk -> "9780066001001".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }

        if (!bookRepository.existsByIsbn("9780066001002")) {
            Book b = new Book();
            b.setIsbn("9780066001002"); b.setTitle("GiftTest Book 149");
            b.setAuthors("Author"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/31-M.jpg");
            b.setCategory(cat); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(149)); b.setStockQuantity(50);
            b.setLanguage("en");
            book149Id = bookRepository.save(b).getId();
        } else {
            book149Id = bookRepository.findAll().stream()
                    .filter(bk -> "9780066001002".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AuthResponseDTO registerUser() throws Exception {
        int n = userCounter.incrementAndGet();
        RegisterRequestDTO req = new RegisterRequestDTO(
                "Gift User " + n,
                "giftuser" + n + "@test.com",
                "60000" + String.format("%05d", n),
                "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponseDTO.class);
    }

    private Long createAddress(Long userId) {
        User user = userRepository.getReferenceById(userId);
        Address addr = new Address();
        addr.setUser(user);
        addr.setStreet("1 Gift St"); addr.setCity("Pune");
        addr.setState("Maharashtra"); addr.setPincode("411001");
        return addressRepository.save(addr).getId();
    }

    private void addToCart(String token, Long bookId, int qty) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":%d,\"quantity\":%d}".formatted(bookId, qty)))
                .andReturn();
    }

    // ------------------------------------------------------------------

    /**
     * TEST-GFT-001
     * Order with grand total ₹540 (₹500 + ₹40 delivery) earns floor(540/50) = 10 points.
     * REQ-GFT-001 AC1
     */
    @Test
    @DisplayName("TEST-GFT-001: Order of ₹500 item (₹540 grand) earns 10 gift points")
    void orderOf500EarnsTenPoints() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        addToCart(auth.token(), book500Id, 1);  // subtotal = ₹500, grand = ₹540
        orderService.createOrder(new CreateOrderDTO(auth.userId(), addressId, 0));

        // floor(540/50) = 10
        int balance = userRepository.findById(auth.userId()).orElseThrow().getGiftPointBalance();
        assertThat(balance).isEqualTo(10);
    }

    /**
     * TEST-GFT-002
     * Order with grand total ₹189 (₹149 + ₹40) earns floor(189/50) = 3 points.
     * REQ-GFT-001 AC2 (spec says "₹149 awards 2 points" but that uses subtotal;
     * spec formula says floor(grandTotal/50) — grand = 189 → floor(189/50) = 3).
     *
     * NOTE: The spec example at REQ-GFT-001 AC2 says "₹149 awards 2 points"
     * which implies the formula uses subtotal not grandTotal. We follow the
     * formula in the spec: floor(grandTotal/50) where grandTotal includes delivery.
     * 189/50 = 3.78 → floor = 3.
     */
    @Test
    @DisplayName("TEST-GFT-002: Order of ₹149 item (₹189 grand) earns 3 gift points")
    void orderOf149EarnsThreePoints() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        addToCart(auth.token(), book149Id, 1);  // subtotal = ₹149, grand = ₹189
        orderService.createOrder(new CreateOrderDTO(auth.userId(), addressId, 0));

        // floor(189/50) = 3
        int balance = userRepository.findById(auth.userId()).orElseThrow().getGiftPointBalance();
        assertThat(balance).isEqualTo(3);
    }

    /**
     * TEST-GFT-003
     * Redeeming 5 points gives ₹10 discount (5 × ₹2 = ₹10).
     * Grand total = ₹500 − ₹10 + ₹40 = ₹530.
     * REQ-GFT-002 AC2, AC4
     */
    @Test
    @DisplayName("TEST-GFT-003: Redeeming 5 points gives ₹10 discount")
    void redeemingFivePointsGivesTenRupeeDiscount() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        // Give user 10 points manually
        User user = userRepository.findById(auth.userId()).orElseThrow();
        user.setGiftPointBalance(10);
        userRepository.save(user);

        addToCart(auth.token(), book500Id, 1);  // subtotal = ₹500
        var order = orderService.createOrder(
                new CreateOrderDTO(auth.userId(), addressId, 5));  // redeem 5 pts

        // discount = 5 × 2 = ₹10, grandTotal = 500 - 10 + 40 = ₹530
        assertThat(order.giftPointsRedeemed()).isEqualTo(5);
        assertThat(order.giftPointDiscount()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(order.grandTotal()).isEqualByComparingTo(BigDecimal.valueOf(530));
    }

    /**
     * TEST-GFT-004
     * Redeeming more points than balance → 400 (IllegalArgumentException).
     * REQ-GFT-002 AC3
     */
    @Test
    @DisplayName("TEST-GFT-004: Redeeming more points than balance throws 400")
    void redeemingMoreThanBalanceThrowsBadRequest() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        // User has 0 points — try to redeem 5
        addToCart(auth.token(), book500Id, 1);

        assertThatThrownBy(() ->
                orderService.createOrder(new CreateOrderDTO(auth.userId(), addressId, 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient gift points");
    }

    /**
     * TEST-GFT-005
     * GET /api/user/giftpoints returns correct balance.
     * REQ-GFT-003 AC1
     */
    @Test
    @DisplayName("TEST-GFT-005: GET /api/user/giftpoints returns correct balance")
    void getGiftPointsReturnsCorrectBalance() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        // Place an order — earns 10 points (₹500 + ₹40 = ₹540, floor(540/50) = 10)
        addToCart(auth.token(), book500Id, 1);
        orderService.createOrder(new CreateOrderDTO(auth.userId(), addressId, 0));

        mockMvc.perform(get("/api/user/giftpoints")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10));
    }

    /**
     * TEST-GFT-006
     * Cancelled order does NOT restore redeemed gift points.
     * REQ-GFT-002, Decision D-010, REQ-ORD-004 AC4
     */
    @Test
    @DisplayName("TEST-GFT-006: Redeemed gift points not restored after cancellation")
    void cancelledOrderDoesNotRestoreRedeemedPoints() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addressId = createAddress(auth.userId());

        // Give user 20 points
        User user = userRepository.findById(auth.userId()).orElseThrow();
        user.setGiftPointBalance(20);
        userRepository.save(user);

        // Place order redeeming 10 points
        addToCart(auth.token(), book500Id, 1);
        var order = orderService.createOrder(
                new CreateOrderDTO(auth.userId(), addressId, 10));

        // Balance after order: 20 - 10 = 10 (redeemed), + floor(530/50)=10 earned = 20
        int balanceAfterOrder = userRepository.findById(auth.userId())
                .orElseThrow().getGiftPointBalance();

        // Cancel the order
        mockMvc.perform(post("/api/orders/" + order.id() + "/cancel")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk());

        // Balance must NOT change — redeemed points are forfeited (D-010)
        int balanceAfterCancel = userRepository.findById(auth.userId())
                .orElseThrow().getGiftPointBalance();
        assertThat(balanceAfterCancel).isEqualTo(balanceAfterOrder);
    }
}
