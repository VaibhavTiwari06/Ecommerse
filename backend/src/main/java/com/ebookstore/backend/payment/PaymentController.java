package com.ebookstore.backend.payment;

import com.ebookstore.backend.auth.JwtService;
import com.ebookstore.backend.payment.dto.PaymentRequestDTO;
import com.ebookstore.backend.payment.dto.PaymentResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PaymentController — HTTP layer for POST /api/payment/initiate.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-PAY-001, REQ-PAY-001–005, design §2.9)
 * =============================================================
 * Single endpoint: accepts a payment request, delegates to
 * PaymentService, returns purchase confirmation.
 *
 * Requires a valid JWT — protected by SecurityConfig catch-all.
 *
 * Invalid paymentMethod (not CREDIT_CARD / DEBIT_CARD) → Jackson
 * throws HttpMessageNotReadableException → GlobalExceptionHandler
 * catches it and returns 400. (REQ-PAY-002 AC2)
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtService     jwtService;

    public PaymentController(PaymentService paymentService,
                             JwtService jwtService) {
        this.paymentService = paymentService;
        this.jwtService     = jwtService;
    }

    /**
     * POST /api/payment/initiate
     *
     * Simulates a payment and creates an order.
     * Returns 200 with purchase confirmation on success.
     *
     * REQ-PAY-001 AC1, REQ-PAY-003, REQ-PAY-004 AC1, REQ-PAY-005 AC2
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponseDTO> initiate(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody PaymentRequestDTO request) {
        Long userId = jwtService.extractUserId(authHeader.substring(7));
        return ResponseEntity.ok(paymentService.initiate(userId, request));
    }
}
