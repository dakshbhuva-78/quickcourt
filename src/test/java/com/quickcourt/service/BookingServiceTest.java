package com.quickcourt.service;

import com.quickcourt.dto.BookingRequest;
import com.quickcourt.dto.SlotDto;
import com.quickcourt.exception.*;
import com.quickcourt.model.*;
import com.quickcourt.repository.*;
import com.quickcourt.util.FileLogger;
import com.quickcourt.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for the booking rules (repositories are mocked, no database needed). */
class BookingServiceTest {

    private BookingRepository bookingRepository;
    private VenueRepository venueRepository;
    private CourtRepository courtRepository;
    private BlockedSlotRepository blockedSlotRepository;
    private PaymentRepository paymentRepository;
    private NotificationService notificationService;
    private BookingService service;

    private Player player;
    private Venue venue;
    private Court court;
    private LocalDate futureDate;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        bookingRepository = mock(BookingRepository.class);
        venueRepository = mock(VenueRepository.class);
        courtRepository = mock(CourtRepository.class);
        blockedSlotRepository = mock(BlockedSlotRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        notificationService = mock(NotificationService.class);
        service = new BookingService(bookingRepository, venueRepository, courtRepository, blockedSlotRepository,
                paymentRepository, notificationService, new FileLogger(tmp.toString()), tmp.toString());

        player = new Player("Aarav", "a@mail.com", "hash");
        player.setId(1L);
        VenueOwner owner = new VenueOwner("Rahul", "r@mail.com", "hash");
        owner.setId(2L);

        venue = new Venue();
        venue.setId(10L);
        venue.setName("Green Arena");
        venue.setOwner(owner);
        venue.setSportType(SportType.BADMINTON);
        venue.setPricePerHour(new BigDecimal("400"));
        venue.setOpeningTime(LocalTime.of(6, 0));
        venue.setClosingTime(LocalTime.of(22, 0));
        venue.setStatus(VenueStatus.APPROVED);

        court = new Court();
        court.setId(20L);
        court.setCourtName("Court 1");
        court.setSportType(SportType.BADMINTON);
        court.setVenue(venue);

        futureDate = TimeUtil.today().plusDays(3);

        when(venueRepository.findById(10L)).thenReturn(Optional.of(venue));
        when(courtRepository.findById(20L)).thenReturn(Optional.of(court));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private BookingRequest request(LocalDate date, String slot) {
        return new BookingRequest(10L, 20L, date, slot);
    }

    private Booking existingBooking(LocalDate date, String slot, BookingStatus status) {
        Booking b = new Booking();
        b.setId(99L);
        b.setUser(player);
        b.setVenue(venue);
        b.setCourt(court);
        b.setBookingDate(date);
        b.setTimeSlot(slot);
        b.setAmount(new BigDecimal("400"));
        b.setBookingStatus(status);
        return b;
    }

    // ---------- creating bookings ----------

    @Test
    void booksAnAvailableSlot() {
        Booking b = service.createBooking(player, request(futureDate, "18:00-19:00"));
        assertEquals(BookingStatus.PENDING, b.getBookingStatus());
        assertEquals(new BigDecimal("400"), b.getAmount());
        assertEquals("18:00-19:00", b.getTimeSlot());
    }

    @Test
    void rejectsAlreadyBookedSlot() {
        when(bookingRepository.existsActiveBooking(eq(20L), eq(futureDate), eq("18:00-19:00"), any())).thenReturn(true);
        assertThrows(SlotUnavailableException.class, () -> service.createBooking(player, request(futureDate, "18:00-19:00")));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void rejectsBlockedSlot() {
        when(blockedSlotRepository.existsBlocked(20L, futureDate, "18:00-19:00")).thenReturn(true);
        assertThrows(SlotUnavailableException.class, () -> service.createBooking(player, request(futureDate, "18:00-19:00")));
    }

    @Test
    void rejectsPastDate() {
        assertThrows(InvalidBookingException.class,
                () -> service.createBooking(player, request(TimeUtil.today().minusDays(1), "18:00-19:00")));
    }

    @Test
    void rejectsTooFarInFuture() {
        assertThrows(InvalidBookingException.class,
                () -> service.createBooking(player, request(TimeUtil.today().plusDays(120), "18:00-19:00")));
    }

    @Test
    void rejectsSlotOutsideOpeningHours() {
        assertThrows(InvalidBookingException.class, () -> service.createBooking(player, request(futureDate, "23:00-24:00")));
    }

    @Test
    void rejectsMalformedSlot() {
        assertThrows(InvalidBookingException.class, () -> service.createBooking(player, request(futureDate, "evening")));
    }

    @Test
    void rejectsUnapprovedVenue() {
        venue.setStatus(VenueStatus.PENDING);
        assertThrows(InvalidBookingException.class, () -> service.createBooking(player, request(futureDate, "18:00-19:00")));
    }

    @Test
    void rejectsUnknownVenue() {
        when(venueRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(VenueNotFoundException.class, () -> service.createBooking(player, request(futureDate, "18:00-19:00")));
    }

    @Test
    void rejectsCourtOfAnotherVenue() {
        Venue other = new Venue();
        other.setId(11L);
        court.setVenue(other);
        assertThrows(InvalidBookingException.class, () -> service.createBooking(player, request(futureDate, "18:00-19:00")));
    }

    @Test
    void onlyPlayersCanBook() {
        assertThrows(ForbiddenException.class, () -> service.createBooking(venue.getOwner(), request(futureDate, "18:00-19:00")));
    }

    // ---------- cancelling ----------

    @Test
    void cancelsOwnFutureBooking() {
        Booking b = existingBooking(futureDate, "18:00-19:00", BookingStatus.CONFIRMED);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(b));
        Booking result = service.cancelBooking(99L, player);
        assertEquals(BookingStatus.CANCELLED, result.getBookingStatus());
        verify(notificationService).sendCancellation(b);
    }

    @Test
    void cannotCancelPastBooking() {
        Booking b = existingBooking(TimeUtil.today().minusDays(1), "18:00-19:00", BookingStatus.CONFIRMED);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingException.class, () -> service.cancelBooking(99L, player));
    }

    @Test
    void cannotCancelTwice() {
        Booking b = existingBooking(futureDate, "18:00-19:00", BookingStatus.CANCELLED);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingException.class, () -> service.cancelBooking(99L, player));
    }

    @Test
    void cannotCancelSomeoneElsesBooking() {
        Booking b = existingBooking(futureDate, "18:00-19:00", BookingStatus.CONFIRMED);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(b));
        Player stranger = new Player("Eve", "e@mail.com", "hash");
        stranger.setId(55L);
        assertThrows(ForbiddenException.class, () -> service.cancelBooking(99L, stranger));
    }

    @Test
    void adminMayCancelAnyBooking() {
        Booking b = existingBooking(futureDate, "18:00-19:00", BookingStatus.CONFIRMED);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(b));
        Admin admin = new Admin("Admin", "ad@mail.com", "hash");
        admin.setId(77L);
        assertEquals(BookingStatus.CANCELLED, service.cancelBooking(99L, admin).getBookingStatus());
    }

    // ---------- availability + completed status ----------

    @Test
    void slotListMarksBookedAndFreeSlots() {
        when(bookingRepository.findActiveForCourtAndDate(eq(20L), eq(futureDate), any()))
                .thenReturn(List.of(existingBooking(futureDate, "18:00-19:00", BookingStatus.CONFIRMED)));
        List<SlotDto> slots = service.getSlots(10L, 20L, futureDate);
        assertEquals(16, slots.size()); // 06:00 to 22:00
        SlotDto booked = slots.stream().filter(s -> s.timeSlot().equals("18:00-19:00")).findFirst().orElseThrow();
        SlotDto free = slots.stream().filter(s -> s.timeSlot().equals("19:00-20:00")).findFirst().orElseThrow();
        assertFalse(booked.available());
        assertEquals("Booked", booked.reason());
        assertTrue(free.available());
    }

    @Test
    void pastDateSlotsAreNotListed() {
        assertThrows(InvalidBookingException.class, () -> service.getSlots(10L, 20L, TimeUtil.today().minusDays(1)));
    }

    @Test
    void confirmedBookingInThePastBecomesCompleted() {
        Booking b = existingBooking(TimeUtil.today().minusDays(2), "18:00-19:00", BookingStatus.CONFIRMED);
        service.markCompleted(List.of(b));
        assertEquals(BookingStatus.COMPLETED, b.getBookingStatus());
    }

    @Test
    void futureConfirmedBookingStaysConfirmed() {
        Booking b = existingBooking(futureDate, "18:00-19:00", BookingStatus.CONFIRMED);
        service.markCompleted(List.of(b));
        assertEquals(BookingStatus.CONFIRMED, b.getBookingStatus());
    }
}
