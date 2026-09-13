package com.ebookstore.backend.payment;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import com.ebookstore.backend.book.Book;
import com.ebookstore.backend.book.BookRepository;
import com.ebookstore.backend.category.CategoryService;
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
 * PaymentApiTest — TEST-PAY-001 through TEST-PAY-006
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M7, TASK-PAY-001, TASK-PAY-002)
 * =============================================================
 *
 * TEST-PAY-001  Valid payment returns 200 with orderId and paymentReference
 * TEST-PAY-002  Invalid paymentMethod ("CASH") returns 400
 * TEST-PAY-003  Redeeming more points than balance returns 400
 * TEST-PAY-004  Grand total = subtotal − giftPointDiscount + 40
 * TEST-PAY-005  Cart is empty after successful payment
 * TEST-PAY-006  Gift points are awarded after payment
 *
 * Uses H2 in-memory DB via @ActiveProfiles("test").
 * Each test registers a unique user for full isolation.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentApiTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private BookRepository        bookRepository;
    @Autowired private CategoryService       categoryService;
    @Autowired private PublisherService      publisherService;
    @Autowired private AddressRepository     addressRepository;
    @Autowired private UserRepository        userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicInteger userCounter = new AtomicInteger(500);

    private MockMvc mockMvc;
    private Long bookId;   // ₹400 in-stock book

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        var cat = categoryService.findOrCreate("PayTest-Category");
        var pub = publisherService.findOrCreate("PayTest-Publisher");

        if (!bookRepository.existsByIsbn("9780055001001")) {
            Book b = new Book();
            b.setIsbn("9780055001001"); b.setTitle("PayTest Book");
            b.setAuthors("Author"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/40-M.jpg");
            b.setCategory(cat); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(400)); b.setStockQuantity(50);
            b.setLanguage("en");
            bookId = bookRepository.save(b).getId();
        } else {
            bookId = bookRepository.findAll().stream()
                    .filter(bk -> "9780055001001".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AuthResponseDTO registerUser() throws Exception {
        int n = userCounter.incrementAndGet();
        RegisterRequestDTO req = new RegisterRequestDTO(
                "Pay User " + n,
                "payuser" + n + "@test.com",
                "50000" + String.format("%05d", n),
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
        addr.setStreet("1 Pay St"); addr.setCity("Bengaluru");
        addr.setState("Karnataka"); addr.setPincode("560001");
        return addressRepository.save(addr).getId();
    }

    private void addToCart(String token, Long bId, int qty) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":%d,\"quantity\":%d}".formatted(bId, qty)))
                .andReturn();
    }

    // ------------------------------------------------------------------

    /**
     * TEST-PAY-001
     * Valid payment request returns 200 with orderId and paymentReference.
     * REQ-PAY-001 AC1, REQ-PAY-004 AC1
     */
    @Test
    @DisplayName("TEST-PAY-001: Valid payment returns 200 with orderId and paymentReference")
    void validPaymentReturns200WithOrderId() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addrId = createAddress(auth.userId());

        addToCart(auth.token(), bookId, 1);

        mockMvc.perform(post("/api/payment/initiate")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":%d,\"paymentMethod\":\"CREDIT_CARD\",\"giftPointsToRedeem\":0}"
                                .formatted(addrId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.paymentReference").value(org.hamcrest.Matchers.startsWith("PAY-")))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.message").value("Purchase successful! Thank you for your order."));
    }

    /**
     * TEST-PAY-002
     * Invalid paymentMethod ("CASH") returns 400.
     * REQ-PAY-002 AC2
     */
    @Test
    @DisplayName("TEST-PAY-002: Invalid paymentMethod returns 400")
    void invalidPaymentMethodReturns400() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addrId = createAddress(auth.userId());

        addToCart(auth.token(), bookId, 1);

        mockMvc.perform(post("/api/payment/initiate")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":%d,\"paymentMethod\":\"CASH\",\"giftPointsToRedeem\":0}"
                                .formatted(addrId)))
                .andExpect(status().isBadRequest());
    }

    /**
     * TEST-PAY-003
     * Redeeming more points than balance returns 400.
     * REQ-GFT-002 AC3
     */
    @Test
    @DisplayName("TEST-PAY-003: Redeeming more points than balance returns 400")
    void redeemingMoreThanBalanceReturns400() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addrId = createAddress(auth.userId());

        // User has 0 points — try redeeming 10
        addToCart(auth.token(), bookId, 1);

        mockMvc.perform(post("/api/payment/initiate")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":%d,\"paymentMethod\":\"DEBIT_CARD\",\"giftPointsToRedeem\":10}"
                                .formatted(addrId)))
                .andExpect(status().isBadRequest());
    }

    /**
     * TEST-PAY-004
     * Grand total = subtotal − giftPointDiscount + 40.
     * ₹400 − ₹10 (5pts × ₹2) + ₹40 = ₹430.
     * REQ-CHK-002 AC2, REQ-GFT-002 AC4
     */
    @Test
    @DisplayName("TEST-PAY-004: Grand total correctly applies gift point discount")
    void grandTotalCorrectlyAppliesGiftPointDiscount() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addrId = createAddress(auth.userId());

        // Give user 10 points
        User user = userRepository.findById(auth.userId()).orElseThrow();
        user.setGiftPointBalance(10);
        userRepository.save(user);

        addToCart(auth.token(), bookId, 1);  // subtotal = ₹400

        mockMvc.perform(post("/api/payment/initiate")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":%d,\"paymentMethod\":\"CREDIT_CARD\",\"giftPointsToRedeem\":5}"
                                .formatted(addrId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(400.0))
                .andExpect(jsonPath("$.giftPointsRedeemed").value(5))
                .andExpect(jsonPath("$.giftPointDiscount").value(10.0))
                .andExpect(jsonPath("$.deliveryCharge").value(40.0))
                // grandTotal = 400 - 10 + 40 = 430
                .andExpect(jsonPath("$.grandTotal").value(430.0));
    }

    /**
     * TEST-PAY-005
     * Cart is empty after successful payment.
     * REQ-PAY-003 AC3, REQ-ORD-001 AC2
     */
    @Test
    @DisplayName("TEST-PAY-005: Cart is empty after successful payment")
    void cartIsEmptyAfterSuccessfulPayment() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addrId = createAddress(auth.userId());

        addToCart(auth.token(), bookId, 2);

        // Verify cart has items before payment
        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + auth.token()))
                .andExpect(jsonPath("$.items.length()").value(1));

        // Pay
        mockMvc.perform(post("/api/payment/initiate")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":%d,\"paymentMethod\":\"DEBIT_CARD\",\"giftPointsToRedeem\":0}"
                                .formatted(addrId)))
                .andExpect(status().isOk());

        // Cart must be empty after payment
        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    /**
     * TEST-PAY-006
     * Gift points are awarded after successful payment.
     * grandTotal = ₹400 + ₹40 = ₹440 → floor(440/50) = 8 points.
     * REQ-GFT-001 AC1, AC3
     */
    @Test
    @DisplayName("TEST-PAY-006: Gift points are awarded after payment")
    void giftPointsAwardedAfterPayment() throws Exception {
        AuthResponseDTO auth = registerUser();
        Long addrId = createAddress(auth.userId());

        addToCart(auth.token(), bookId, 1);  // grandTotal = ₹440

        mockMvc.perform(post("/api/payment/initiate")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":%d,\"paymentMethod\":\"CREDIT_CARD\",\"giftPointsToRedeem\":0}"
                                .formatted(addrId)))
                .andExpect(status().isOk())
                // floor(440/50) = 8
                .andExpect(jsonPath("$.giftPointsEarned").value(8));

        // Also verify the DB balance is correct
        int balance = userRepository.findById(auth.userId()).orElseThrow().getGiftPointBalance();
        assertThat(balance).isEqualTo(8);
    }
}
