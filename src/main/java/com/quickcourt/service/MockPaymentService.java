package com.quickcourt.service;

import com.quickcourt.exception.InvalidBookingException;
import com.quickcourt.exception.InvalidRequestException;
import com.quickcourt.exception.ResourceNotFoundException;
import com.quickcourt.model.*;
import com.quickcourt.repository.BookingRepository;
import com.quickcourt.repository.PaymentRepository;
import com.quickcourt.util.FileLogger;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * SIMULATED payment gateway. No real money moves.
 * Rules (so you can demonstrate success AND failure):
 *   UPI  : id must look like name@bank. If it starts with "fail" -> FAILED, otherwise SUCCESS
 *   CARD : needs 16 digits. If the number ends with 0000 -> FAILED (declined), otherwise SUCCESS
 *   CASH : payment stays PENDING (pay at the venue) but the booking is CONFIRMED
 * Booking becomes CONFIRMED after SUCCESS (or CASH). After FAILED it stays PENDING so the user can retry.
 */
@Service
public class MockPaymentService implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final FileLogger fileLogger;

    public MockPaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository,
                              NotificationService notificationService, FileLogger fileLogger) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.fileLogger = fileLogger;
    }

    @Override
    public Payment processPayment(Booking booking, PaymentMethod method, String upiId, String cardNumber) {
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("A cancelled booking cannot be paid");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingException("This booking is already confirmed");
        }

        PaymentStatus status = decideStatus(method, upiId, cardNumber);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getAmount());
        payment.setPaymentMethod(method);
        payment.setPaymentStatus(status);
        payment.setTransactionReference("QC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        Payment saved = paymentRepository.save(payment);

        if (status == PaymentStatus.SUCCESS || method == PaymentMethod.CASH) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            notificationService.sendBookingConfirmation(booking); // runs in a background thread
        }
        fileLogger.info("Payment " + saved.getTransactionReference() + " for booking #" + booking.getId()
                + " via " + method + " -> " + status);
        return saved;
    }

    @Override
    public Payment getPaymentForBooking(Long bookingId) {
        return paymentRepository.findLatestForBooking(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for booking " + bookingId));
    }

    private PaymentStatus decideStatus(PaymentMethod method, String upiId, String cardNumber) {
        if (method == PaymentMethod.CASH) {
            return PaymentStatus.PENDING;
        }
        if (method == PaymentMethod.UPI) {
            if (upiId == null || !upiId.trim().matches("^[A-Za-z0-9._\\-]{2,}@[A-Za-z]{2,}$")) {
                throw new InvalidRequestException("Enter a valid UPI id, for example name@bank");
            }
            return upiId.trim().toLowerCase().startsWith("fail") ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
        }
        // CARD
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        if (digits.length() != 16) {
            throw new InvalidRequestException("Card number must have 16 digits");
        }
        return digits.endsWith("0000") ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
    }
}
