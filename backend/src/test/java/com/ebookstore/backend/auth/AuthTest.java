package com.ebookstore.backend.auth;

import com.ebookstore.backend.auth.dto.AuthResponseDTO;
import com.ebookstore.backend.auth.dto.LoginRequestDTO;
import com.ebookstore.backend.auth.dto.RegisterRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ObjectMapper is created directly — not autowired — because Spring Boot's
// web auto-configuration doesn't register it as a bean in plain @SpringBootTest
// (no embedded server, no web slice). Jackson is on the classpath from
// spring-boot-starter-json which is pulled by spring-boot-starter-webmvc.

/**
 * AuthTest — TEST-AUTH-001 through TEST-AUTH-007
 *
 * =============================================================
 * WHAT THIS TEST CLASS VERIFIES (M2, TASK-AUTH-002, TASK-AUTH-003)
 * =============================================================
 * Tests the full authentication flow:
 *
 * TEST-AUTH-001  Register with valid data returns 200 + JWT
 * TEST-AUTH-002  Register with duplicate email returns 409
 * TEST-AUTH-002b Register with duplicate phone returns 409
 * TEST-AUTH-002c Register with invalid phone number returns 400
 * TEST-AUTH-003  Login with email + password returns JWT
 * TEST-AUTH-003b Login with phone number + password returns JWT
 * TEST-AUTH-004  Login with wrong password returns 401
 * TEST-AUTH-005  Protected endpoint without JWT returns 401  (already in FoundationTest)
 * TEST-AUTH-006  Protected endpoint with valid JWT returns 200
 *
 * Uses H2 in-memory database via @ActiveProfiles("test").
 * Each test method runs in a transaction that rolls back afterward
 * to keep tests isolated — EXCEPT we use @SpringBootTest which
 * uses the full context; see note below on test isolation.
 *
 * NOTE ON TEST ISOLATION:
 *   We use unique email/phone values in each test to avoid state
 *   conflicts between tests since we don't roll back between tests here.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    // Create ObjectMapper directly — Jackson is on the classpath via
    // spring-boot-starter-webmvc → spring-boot-starter-json
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    /** Helper to perform a registration request. */
    private MvcResult register(String fullName, String email,
                               String phone, String password) throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO(fullName, email, phone, password);
        return mockMvc().perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    /** Helper to perform a login request. */
    private MvcResult login(String identifier, String password) throws Exception {
        LoginRequestDTO req = new LoginRequestDTO(identifier, password);
        return mockMvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    // ------------------------------------------------------------------

    /**
     * TEST-AUTH-001
     * Valid registration returns 200 with a JWT token.
     * The response must include a non-empty token, userId, and fullName.
     * REQ-USR-004 AC4
     */
    @Test
    @DisplayName("TEST-AUTH-001: Register with valid data returns 200 + JWT")
    void registerWithValidDataReturns200AndToken() throws Exception {
        MvcResult result = register("Test User", "auth001@test.com", "9000000001", "password123");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        AuthResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponseDTO.class);

        assertThat(response.token()).isNotBlank();
        assertThat(response.userId()).isPositive();
        assertThat(response.fullName()).isEqualTo("Test User");
    }

    /**
     * TEST-AUTH-002
     * Registering with a duplicate email returns 409 Conflict.
     * REQ-USR-004 AC1
     */
    @Test
    @DisplayName("TEST-AUTH-002: Duplicate email returns 409")
    void registerWithDuplicateEmailReturns409() throws Exception {
        register("User A", "dup002@test.com", "9000000002", "password123");

        MvcResult result = register("User B", "dup002@test.com", "9000000099", "password456");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
    }

    /**
     * TEST-AUTH-002b
     * Registering with a duplicate phone number returns 409 Conflict.
     * REQ-USR-004 AC2, CR-001
     */
    @Test
    @DisplayName("TEST-AUTH-002b: Duplicate phone returns 409")
    void registerWithDuplicatePhoneReturns409() throws Exception {
        register("User A", "phone002a@test.com", "9000000003", "password123");

        MvcResult result = register("User B", "phone002b@test.com", "9000000003", "password456");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
    }

    /**
     * TEST-AUTH-002c
     * Registering with an invalid phone number (9 digits) returns 400.
     * REQ-USR-004 AC6
     */
    @Test
    @DisplayName("TEST-AUTH-002c: Invalid phone number (9 digits) returns 400")
    void registerWithInvalidPhoneReturns400() throws Exception {
        MvcResult result = register("User C", "phone002c@test.com", "900000000", "password123");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    /**
     * TEST-AUTH-003
     * Login with email + password returns 200 + JWT.
     * REQ-USR-005 AC1
     */
    @Test
    @DisplayName("TEST-AUTH-003: Login with email returns JWT")
    void loginWithEmailReturnsJwt() throws Exception {
        register("Login User", "login003@test.com", "9000000004", "mypassword");

        MvcResult result = login("login003@test.com", "mypassword");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        AuthResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponseDTO.class);
        assertThat(response.token()).isNotBlank();
    }

    /**
     * TEST-AUTH-003b
     * Login with phone number + password returns 200 + JWT.
     * REQ-USR-005 AC2, CR-001
     */
    @Test
    @DisplayName("TEST-AUTH-003b: Login with phone number returns JWT")
    void loginWithPhoneReturnsJwt() throws Exception {
        register("Phone Login User", "login003b@test.com", "9000000005", "mypassword");

        MvcResult result = login("9000000005", "mypassword");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        AuthResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponseDTO.class);
        assertThat(response.token()).isNotBlank();
    }

    /**
     * TEST-AUTH-004
     * Login with wrong password returns 401 Unauthorized.
     * REQ-USR-005 AC3
     */
    @Test
    @DisplayName("TEST-AUTH-004: Wrong password returns 401")
    void loginWithWrongPasswordReturns401() throws Exception {
        register("Wrong Pass User", "wrongpass@test.com", "9000000006", "correctPassword");

        MvcResult result = login("wrongpass@test.com", "wrongPassword");

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    /**
     * TEST-AUTH-006
     * A valid JWT allows access to a protected endpoint.
     * REQ-USR-005 AC4 (inverse: with JWT → 200, not 401)
     */
    @Test
    @DisplayName("TEST-AUTH-006: Valid JWT allows access to protected endpoint")
    void validJwtAllowsAccessToProtectedEndpoint() throws Exception {
        // Register and get a token
        MvcResult regResult = register("JWT User", "jwtuser@test.com", "9000000007", "password123");
        AuthResponseDTO authResponse = objectMapper.readValue(
                regResult.getResponse().getContentAsString(), AuthResponseDTO.class);
        String token = authResponse.token();

        // Use the token to access the protected profile endpoint
        mockMvc().perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("JWT User"))
                .andExpect(jsonPath("$.email").value("jwtuser@test.com"));
    }

    /**
     * TEST-AUTH-007
     * Add an address and list addresses returns the correct count.
     * REQ-USR-006 AC1, AC2
     */
    @Test
    @DisplayName("TEST-AUTH-007: Add address then list returns it")
    void addAddressThenListReturnsIt() throws Exception {
        // Register and login
        MvcResult regResult = register("Addr User", "addruser@test.com", "9000000008", "password123");
        String token = objectMapper.readValue(
                regResult.getResponse().getContentAsString(), AuthResponseDTO.class).token();

        // Add first address
        String addressJson = """
                {
                  "label": "Home",
                  "street": "123 MG Road",
                  "city": "Mumbai",
                  "state": "Maharashtra",
                  "pincode": "400001",
                  "isDefault": true
                }
                """;

        mockMvc().perform(post("/api/user/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Mumbai"));

        // Add second address
        String address2Json = """
                {
                  "label": "Office",
                  "street": "456 Park Street",
                  "city": "Pune",
                  "state": "Maharashtra",
                  "pincode": "411001",
                  "isDefault": false
                }
                """;

        mockMvc().perform(post("/api/user/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(address2Json))
                .andExpect(status().isCreated());

        // List addresses — should return 2
        mockMvc().perform(get("/api/user/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
