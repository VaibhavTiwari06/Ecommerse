package com.ebookstore.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FoundationTest — TEST-FOUND-001
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (M1, TASK-FOUND-001 through TASK-FOUND-003)
 * =============================================================
 * 1. The Spring application context loads without errors.
 *    → If any bean is misconfigured, the context fails to start
 *      and this test fails with a clear error message.
 *
 * 2. /actuator/health is publicly accessible and returns UP.
 *    → Verifies Actuator is wired correctly (TASK-FOUND-001).
 *
 * 3. A protected endpoint (/api/cart) returns HTTP 401 without JWT.
 *    → Verifies Spring Security is blocking unauthenticated access
 *      (TASK-FOUND-003, REQ-NFR-001).
 *
 * 4. A public endpoint (/api/books) does NOT return 401.
 *    → Verifies public routes are accessible without a token
 *      (TASK-FOUND-003, REQ-USR-001).
 *
 * PROFILE:
 *   @ActiveProfiles("test") activates application-test.properties
 *   which uses H2 in-memory DB — no real PostgreSQL needed.
 *
 * SETUP:
 *   MockMvc is built manually with springSecurity() applied so
 *   Spring Security's filter chain participates in test requests.
 */
@SpringBootTest
@ActiveProfiles("test")
class FoundationTest {

    /**
     * WebApplicationContext gives us access to the full Spring MVC +
     * Spring Security setup so we can test the entire filter chain.
     */
    @Autowired
    private WebApplicationContext webApplicationContext;

    /**
     * ApplicationContext lets us verify beans are present.
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Build MockMvc with Spring Security applied.
     * This is the correct way to test secured endpoints without
     * starting a real HTTP server — it simulates real requests
     * through the full filter chain including CORS and JWT filters.
     */
    private MockMvc mockMvc() {
        return MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    // ------------------------------------------------------------------

    /**
     * TEST 1 — Context loads.
     * The most basic test: if the Spring context fails to start
     * (misconfigured bean, missing property, etc.), this test fails
     * immediately with a clear diagnostic message.
     */
    @Test
    @DisplayName("Spring context loads without errors")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    /**
     * TEST 2 — Health endpoint is public and returns UP.
     * /actuator/health must be accessible without a JWT (TASK-FOUND-001).
     * Response body must contain {"status":"UP"}.
     */
    @Test
    @DisplayName("GET /actuator/health returns 200 with status UP")
    void healthEndpointIsPublicAndReturnsUp() throws Exception {
        mockMvc().perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * TEST 3 — Protected endpoint returns 401 without a JWT.
     * /api/cart is a protected endpoint (REQ-CRT-001).
     * Without an Authorization: Bearer token, Spring Security
     * must reject the request with 401 Unauthorized.
     * This verifies TASK-FOUND-003 is working.
     */
    @Test
    @DisplayName("GET /api/cart without JWT returns 401 Unauthorized")
    void protectedEndpointReturns401WithoutJwt() throws Exception {
        mockMvc().perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * TEST 4 — Public catalogue endpoint does NOT require a JWT.
     * /api/books is public (REQ-USR-001, design §3.4).
     * It must not return 401 even without a token.
     * (It may return 404/500 before book controller exists — that is fine.)
     */
    @Test
    @DisplayName("GET /api/books without JWT does not return 401")
    void publicCatalogueIsAccessibleWithoutJwt() throws Exception {
        int status = mockMvc().perform(get("/api/books"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).as("Public endpoint must not require authentication")
                .isNotEqualTo(401);
    }
}
