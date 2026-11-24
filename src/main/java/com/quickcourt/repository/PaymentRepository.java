package com.quickcourt.repository;

import com.quickcourt.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("select p from Payment p where p.booking.id = :bookingId order by p.id desc")
    List<Payment> findByBookingIdNewestFirst(@Param("bookingId") Long bookingId);

    default Optional<Payment> findLatestForBooking(Long bookingId) {
        return findByBookingIdNewestFirst(bookingId).stream().findFirst();
    }
}
