package com.quickcourt.controller;

import com.quickcourt.dto.PaymentRequest;
import com.quickcourt.exception.ForbiddenException;
import com.quickcourt.model.Booking;
import com.quickcourt.model.Payment;
import com.quickcourt.model.User;
import com.quickcourt.service.BookingService;
import com.quickcourt.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Simulated payment endpoints. The controller only knows the PaymentService INTERFACE. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    public PaymentController(PaymentService paymentService, BookingService bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment pay(@RequestAttribute("currentUser") User user, @Valid @RequestBody PaymentRequest request) {
        Booking booking = bookingService.findBooking(request.bookingId());
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only pay for your own bookings");
        }
        return paymentService.processPayment(booking, request.paymentMethod(), request.upiId(), request.cardNumber());
    }

    @GetMapping("/booking/{bookingId}")
    public Payment forBooking(@RequestAttribute("currentUser") User user, @PathVariable Long bookingId) {
        bookingService.getBookingFor(bookingId, user); // permission check
        return paymentService.getPaymentForBooking(bookingId);
    }
}
