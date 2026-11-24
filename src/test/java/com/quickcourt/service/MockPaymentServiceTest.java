package com.quickcourt.service;

import com.quickcourt.exception.InvalidBookingException;
import com.quickcourt.exception.InvalidRequestException;
import com.quickcourt.model.*;
import com.quickcourt.repository.BookingRepository;
import com.quickcourt.repository.PaymentRepository;
import com.quickcourt.util.FileLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for the simulated payment rules (success, failure, cash, validation). */
class MockPaymentServiceTest {

    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private NotificationService notificationService;
    private MockPaymentService service;
    private Booking booking;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        notificationService = mock(NotificationService.class);
        service = new MockPaymentService(paymentRepository, bookingRepository, notificationService, new FileLogger(tmp.toString()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        booking = new Booking();
        booking.setId(1L);
        booking.setAmount(new BigDecimal("400"));
        booking.setBookingStatus(BookingStatus.PENDING);
    }

    @Test
    void upiPaymentSucceeds() {
        Payment p = service.processPayment(booking, PaymentMethod.UPI, "aarav@okbank", null);
        assertEquals(PaymentStatus.SUCCESS, p.getPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(new BigDecimal("400"), p.getAmount());
        verify(notificationService).sendBookingConfirmation(booking);
    }

    @Test
    void upiIdStartingWithFailIsDeclined() {
        Payment p = service.processPayment(booking, PaymentMethod.UPI, "fail@upi", null);
        assertEquals(PaymentStatus.FAILED, p.getPaymentStatus());
        assertEquals(BookingStatus.PENDING, booking.getBookingStatus());   // user may retry
        verify(notificationService, never()).sendBookingConfirmation(any());
    }

    @Test
    void invalidUpiIdIsRejected() {
        assertThrows(InvalidRequestException.class, () -> service.processPayment(booking, PaymentMethod.UPI, "not-an-upi", null));
        assertThrows(InvalidRequestException.class, () -> service.processPayment(booking, PaymentMethod.UPI, null, null));
    }

    @Test
    void cardPaymentSucceeds() {
        Payment p = service.processPayment(booking, PaymentMethod.CARD, null, "4111 1111 1111 1111");
        assertEquals(PaymentStatus.SUCCESS, p.getPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
    }

    @Test
    void cardEndingWithZerosIsDeclined() {
        Payment p = service.processPayment(booking, PaymentMethod.CARD, null, "4111111111110000");
        assertEquals(PaymentStatus.FAILED, p.getPaymentStatus());
        assertEquals(BookingStatus.PENDING, booking.getBookingStatus());
    }

    @Test
    void cardWithWrongLengthIsRejected() {
        assertThrows(InvalidRequestException.class, () -> service.processPayment(booking, PaymentMethod.CARD, null, "12345678"));
    }

    @Test
    void cashKeepsPaymentPendingButConfirmsBooking() {
        Payment p = service.processPayment(booking, PaymentMethod.CASH, null, null);
        assertEquals(PaymentStatus.PENDING, p.getPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
    }

    @Test
    void cancelledBookingCannotBePaid() {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        assertThrows(InvalidBookingException.class, () -> service.processPayment(booking, PaymentMethod.UPI, "a@bank", null));
    }

    @Test
    void alreadyConfirmedBookingCannotBePaidAgain() {
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        assertThrows(InvalidBookingException.class, () -> service.processPayment(booking, PaymentMethod.UPI, "a@bank", null));
    }

    @Test
    void transactionReferenceIsGenerated() {
        Payment p = service.processPayment(booking, PaymentMethod.UPI, "a@bank", null);
        assertNotNull(p.getTransactionReference());
        assertTrue(p.getTransactionReference().startsWith("QC-"));
    }
}
