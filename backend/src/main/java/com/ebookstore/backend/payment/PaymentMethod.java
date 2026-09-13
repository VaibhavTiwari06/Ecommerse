package com.ebookstore.backend.payment;

/**
 * PaymentMethod — the two accepted payment methods.
 *
 * REQ-PAY-002 AC1: only CREDIT_CARD and DEBIT_CARD are valid.
 * Any other value in the request → 400 (Jackson enum deserialisation
 * will throw an exception caught by GlobalExceptionHandler → 400).
 */
public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD
}
