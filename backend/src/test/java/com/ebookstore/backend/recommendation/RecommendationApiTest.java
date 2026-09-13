package com.ebookstore.backend.recommendation;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import com.ebookstore.backend.book.Book;
import com.ebookstore.backend.book.BookRepository;
import com.ebookstore.backend.category.Category;
import com.ebookstore.backend.category.CategoryService;
import com.ebookstore.backend.order.OrderService;
import com.ebookstore.backend.order.dto.CreateOrderDTO;
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
 * RecommendationApiTest — TEST-REC-001 through TEST-REC-005
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M9, TASK-REC-001, TASK-REC-002)
 * =============================================================
 *
 * TEST-REC-001  User with orders gets recommendations from same category
 * TEST-REC-002  Purchased books do NOT appear in recommendations
 * TEST-REC-003  User with no orders → 200 with empty list
 * TEST-REC-004  limit=8 returns at most 8 books
 * TEST-REC-005  limit=4 returns at most 4 books
 *
 * Test data layout:
 *   recCatA    — category A (purchased)
 *   recCatB    — category B (not purchased → not recommended unless author match)
 *   bookA1     — category A, author "Rec Author A" → purchased by test user
 *   bookA2     — category A, author "Rec Author A" → NOT purchased → score 3 (cat+author)
 *   bookA3..A9 — category A, different author     → NOT purchased → score 2 (cat only)
 *   bookB1     — category B, unrelated author     → NOT purchased → score 0 (no match)
 *
 * Uses H2 in-memory DB via @ActiveProfiles("test").
 */
@SpringBootTest
@ActiveProfiles("test")
class RecommendationApiTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private BookRepository        bookRepository;
    @Autowired private CategoryService       categoryService;
    @Autowired private PublisherService      publisherService;
    @Autowired private AddressRepository     addressRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private OrderService          orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicInteger userCounter = new AtomicInteger(400);

    private MockMvc mockMvc;

    // Shared book IDs
    private Long bookA1Id;   // purchased — category A, author "Rec Author A"
    private Long bookA2Id;   // NOT purchased — category A, same author → score 3
    private Long bookB1Id;   // NOT purchased — category B, different author → score 0

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        var pub = publisherService.findOrCreate("RecTest-Publisher");
        Category catA = categoryService.findOrCreate("RecTest-CategoryA");
        Category catB = categoryService.findOrCreate("RecTest-CategoryB");

        if (!bookRepository.existsByIsbn("9780044001001")) {
            Book b = new Book();
            b.setIsbn("9780044001001"); b.setTitle("RecTest BookA1 (to buy)");
            b.setAuthors("Rec Author A"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/50-M.jpg");
            b.setCategory(catA); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(300)); b.setStockQuantity(10);
            b.setLanguage("en");
            bookA1Id = bookRepository.save(b).getId();
        } else {
            bookA1Id = bookRepository.findAll().stream()
                    .filter(bk -> "9780044001001".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }

        if (!bookRepository.existsByIsbn("9780044001002")) {
            Book b = new Book();
            b.setIsbn("9780044001002"); b.setTitle("RecTest BookA2 (not bought)");
            b.setAuthors("Rec Author A"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/51-M.jpg");
            b.setCategory(catA); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(350)); b.setStockQuantity(10);
            b.setLanguage("en");
            bookA2Id = bookRepository.save(b).getId();
        } else {
            bookA2Id = bookRepository.findAll().stream()
                    .filter(bk -> "9780044001002".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }

        if (!bookRepository.existsByIsbn("9780044001003")) {
            Book b = new Book();
            b.setIsbn("9780044001003"); b.setTitle("RecTest BookB1 (unrelated)");
            b.setAuthors("Unrelated Author Z"); b.setDescription("desc");
            b.setCoverImageUrl("https://covers.openlibrary.org/b/id/52-M.jpg");
            b.setCategory(catB); b.setPublisher(pub);
            b.setPrice(BigDecimal.valueOf(200)); b.setStockQuantity(10);
            b.setLanguage("en");
            bookB1Id = bookRepository.save(b).getId();
        } else {
            bookB1Id = bookRepository.findAll().stream()
                    .filter(bk -> "9780044001003".equals(bk.getIsbn()))
                    .findFirst().map(Book::getId).orElseThrow();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AuthResponseDTO registerUser() throws Exception {
        int n = userCounter.incrementAndGet();
        RegisterRequestDTO req = new RegisterRequestDTO(
                "Rec User " + n,
                "recuser" + n + "@test.com",
                "40000" + String.format("%05d", n),
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
        addr.setStreet("1 Rec St"); addr.setCity("Hyderabad");
        addr.setState("Telangana"); addr.setPincode("500001");
        return addressRepository.save(addr).getId();
    }

    private void addToCartAndOrder(String token, Long userId, Long bId) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":%d,\"quantity\":1}".formatted(bId)))
                .andReturn();
        Long addrId = createAddress(userId);
        orderService.createOrder(new CreateOrderDTO(userId, addrId, 0));
    }

    // ------------------------------------------------------------------

    /**
     * TEST-REC-001
     * User who purchased bookA1 (category A) should see bookA2 (same category)
     * in recommendations. REQ-REC-001 AC1, AC3
     */
    @Test
    @DisplayName("TEST-REC-001: User with orders gets recommendations from same category")
    void userWithOrdersGetsCategoryRecommendations() throws Exception {
        AuthResponseDTO auth = registerUser();

        // Purchase bookA1 → history in category A
        addToCartAndOrder(auth.token(), auth.userId(), bookA1Id);

        MvcResult result = mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // bookA2 is in category A (same as purchased) → must appear
        assertThat(body).contains("RecTest BookA2 (not bought)");
    }

    /**
     * TEST-REC-002
     * Purchased books must NOT appear in recommendations. REQ-REC-001 AC2
     */
    @Test
    @DisplayName("TEST-REC-002: Purchased books do not appear in recommendations")
    void purchasedBooksNotInRecommendations() throws Exception {
        AuthResponseDTO auth = registerUser();

        addToCartAndOrder(auth.token(), auth.userId(), bookA1Id);

        MvcResult result = mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // bookA1 was purchased — must NOT appear
        assertThat(body).doesNotContain("RecTest BookA1 (to buy)");
    }

    /**
     * TEST-REC-003
     * User with no orders receives empty list (200 OK, not 404).
     * REQ-REC-001 AC4
     */
    @Test
    @DisplayName("TEST-REC-003: User with no orders receives empty list")
    void userWithNoOrdersGetsEmptyList() throws Exception {
        AuthResponseDTO auth = registerUser();

        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * TEST-REC-004
     * limit=8 returns at most 8 books. REQ-REC-002 AC1
     */
    @Test
    @DisplayName("TEST-REC-004: limit=8 returns at most 8 books")
    void limitEightReturnsAtMostEight() throws Exception {
        AuthResponseDTO auth = registerUser();

        addToCartAndOrder(auth.token(), auth.userId(), bookA1Id);

        MvcResult result = mockMvc.perform(get("/api/recommendations?limit=8")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andReturn();

        // Parse array length
        var arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.size()).isLessThanOrEqualTo(8);
    }

    /**
     * TEST-REC-005
     * limit=4 returns at most 4 books. REQ-REC-002 AC2
     */
    @Test
    @DisplayName("TEST-REC-005: limit=4 returns at most 4 books")
    void limitFourReturnsAtMostFour() throws Exception {
        AuthResponseDTO auth = registerUser();

        addToCartAndOrder(auth.token(), auth.userId(), bookA1Id);

        MvcResult result = mockMvc.perform(get("/api/recommendations?limit=4")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andReturn();

        var arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.size()).isLessThanOrEqualTo(4);
    }

    /**
     * TEST-REC-006
     * Unrelated book (category B, different author) does NOT appear when
     * user only purchased from category A. REQ-REC-001 AC3
     */
    @Test
    @DisplayName("TEST-REC-006: Unrelated book (different category+author) not recommended")
    void unrelatedBookNotRecommended() throws Exception {
        AuthResponseDTO auth = registerUser();

        addToCartAndOrder(auth.token(), auth.userId(), bookA1Id);

        MvcResult result = mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // bookB1 is category B with a different author → score 0 → not included
        assertThat(body).doesNotContain("RecTest BookB1 (unrelated)");
    }
}
