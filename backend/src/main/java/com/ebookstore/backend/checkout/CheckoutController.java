package com.ebookstore.backend.checkout;

import com.ebookstore.backend.auth.JwtService;
import com.ebookstore.backend.checkout.dto.CheckoutSummaryDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CheckoutController — HTTP layer for GET /api/checkout/summary.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CHK-001, REQ-CHK-001–003, design §2.8)
 * =============================================================
 * Single endpoint: assembles everything the checkout page needs
 * — cart, totals, addresses, gift point balance, delivery date.
 *
 * Requires a valid JWT (enforced by SecurityConfig — all /api/checkout/**
 * is protected under the catch-all .anyRequest().authenticated() rule).
 *
 * Returns 200 even for an empty cart or no saved addresses —
 * neither is an error state, the UI handles them as prompts.
 */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final JwtService      jwtService;

    public CheckoutController(CheckoutService checkoutService,
                              JwtService jwtService) {
        this.checkoutService = checkoutService;
        this.jwtService      = jwtService;
    }

    /**
     * GET /api/checkout/summary
     *
     * Returns the full checkout payload for the authenticated user.
     * REQ-CHK-001 AC2, REQ-CHK-002 AC1, REQ-CHK-003 AC1
     */
    @GetMapping("/summary")
    public ResponseEntity<CheckoutSummaryDTO> getSummary(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        return ResponseEntity.ok(checkoutService.getSummary(userId));
    }
}
