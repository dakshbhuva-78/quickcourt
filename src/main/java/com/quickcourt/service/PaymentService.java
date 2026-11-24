package com.quickcourt.service;

import com.quickcourt.model.Booking;
import com.quickcourt.model.Payment;
import com.quickcourt.model.PaymentMethod;

/**
 * INTERFACE: describes WHAT a payment service must do, not HOW.
 * Today we use MockPaymentService (simulation). Later a RazorpayPaymentService
 * could implement the same interface without changing the controller or booking code.
 */
public interface PaymentService {

    /** Tries to pay for the booking and returns the saved Payment (SUCCESS / FAILED / PENDING). */
    Payment processPayment(Booking booking, PaymentMethod method, String upiId, String cardNumber);

    /** Latest payment of a booking, or throws ResourceNotFoundException. */
    Payment getPaymentForBooking(Long bookingId);
}
