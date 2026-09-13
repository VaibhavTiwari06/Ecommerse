package com.ebookstore.backend.exception;

/**
 * InsufficientGiftPointsException — thrown when a user tries to redeem
 * more gift points than their current balance.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-GFT-002, design §3.5)
 * =============================================================
 * Per spec REQ-GFT-002 AC3: "A user cannot redeem more points
 * than their current balance — returns 400."
 *
 * Rather than doing a manual balance check and returning a raw
 * ResponseEntity in every place, we throw this exception and let
 * the GlobalExceptionHandler handle the HTTP response uniformly.
 *
 * The GlobalExceptionHandler maps this → HTTP 400 Bad Request.
 *
 * Usage example:
 *   if (pointsToRedeem > user.getGiftPointBalance()) {
 *       throw new InsufficientGiftPointsException(
 *           "Insufficient gift points. Balance: " + balance +
 *           ", Requested: " + pointsToRedeem);
 *   }
 */
public class InsufficientGiftPointsException extends RuntimeException {

    /**
     * @param message describes the shortfall.
     *                This message IS sent to the client.
     */
    public InsufficientGiftPointsException(String message) {
        super(message);
    }
}
