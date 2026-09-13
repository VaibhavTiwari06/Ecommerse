package com.ebookstore.backend.payment.dto;

import com.ebookstore.backend.payment.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * PaymentRequestDTO — request body for POST /api/payment/initiate.
 *
 * REQ-PAY-001 AC1, REQ-PAY-002 AC1
 *
 * @param addressId          delivery address (must belong to authenticated user)
 * @param paymentMethod      CREDIT_CARD or DEBIT_CARD (REQ-PAY-002)
 * @param giftPointsToRedeem points to apply as discount (0 = none, REQ-GFT-002)
 */
public record PaymentRequestDTO(
        @NotNull(message = "Address is required")
        Long addressId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Min(value = 0, message = "Gift points to redeem cannot be negative")
        int giftPointsToRedeem
) {}
