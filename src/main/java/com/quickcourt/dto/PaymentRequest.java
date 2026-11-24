package com.quickcourt.dto;

import com.quickcourt.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/**
 * SIMULATED payment input.
 * upiId is needed for UPI, cardNumber for CARD (used only to decide success/failure, never stored).
 */
public record PaymentRequest(
        @NotNull(message = "Booking is required") Long bookingId,
        @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
        String upiId,
        String cardNumber) {
}
