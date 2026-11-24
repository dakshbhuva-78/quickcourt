package com.quickcourt.repository;

import com.quickcourt.model.Booking;
import com.quickcourt.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Used to prevent double booking of the same court + date + slot. */
    @Query("select count(b) from Booking b where b.court.id = :courtId and b.bookingDate = :date "
            + "and b.timeSlot = :slot and b.bookingStatus in :statuses")
    long countActiveBookings(@Param("courtId") Long courtId, @Param("date") LocalDate date,
                             @Param("slot") String slot, @Param("statuses") Collection<BookingStatus> statuses);

    default boolean existsActiveBooking(Long courtId, LocalDate date, String slot, Collection<BookingStatus> statuses) {
        return countActiveBookings(courtId, date, slot, statuses) > 0;
    }

    @Query("select b from Booking b where b.court.id = :courtId and b.bookingDate = :date "
            + "and b.bookingStatus in :statuses")
    List<Booking> findActiveForCourtAndDate(@Param("courtId") Long courtId, @Param("date") LocalDate date,
                                            @Param("statuses") Collection<BookingStatus> statuses);

    @Query("select b from Booking b where b.user.id = :userId order by b.bookingDate desc, b.timeSlot desc")
    List<Booking> findByUser(@Param("userId") Long userId);

    @Query("select b from Booking b where b.venue.owner.id = :ownerId order by b.bookingDate desc, b.timeSlot desc")
    List<Booking> findByOwner(@Param("ownerId") Long ownerId);

    @Query("select b from Booking b order by b.bookingDate desc, b.timeSlot desc")
    List<Booking> findAllSorted();
}
