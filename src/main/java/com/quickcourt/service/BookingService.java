package com.quickcourt.service;

import com.quickcourt.dto.BookingRequest;
import com.quickcourt.dto.SlotDto;
import com.quickcourt.exception.*;
import com.quickcourt.model.*;
import com.quickcourt.repository.*;
import com.quickcourt.util.CsvExporter;
import com.quickcourt.util.FileLogger;
import com.quickcourt.util.SlotUtil;
import com.quickcourt.util.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The heart of QuickCourt: availability, booking, cancellation and CSV export.
 *
 * DOUBLE-BOOKING PREVENTION: createBooking() is "synchronized" (one thread at a time) and checks
 * the database for an existing PENDING/CONFIRMED/COMPLETED booking of the same court + date + slot.
 */
@Service
public class BookingService {

    /** Booking statuses that occupy a slot. CANCELLED bookings free the slot again. */
    private static final List<BookingStatus> OCCUPYING =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
    private static final int MAX_ADVANCE_DAYS = 90;

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final FileLogger fileLogger;
    private final String exportDir;

    public BookingService(BookingRepository bookingRepository, VenueRepository venueRepository,
                          CourtRepository courtRepository, BlockedSlotRepository blockedSlotRepository,
                          PaymentRepository paymentRepository, NotificationService notificationService,
                          FileLogger fileLogger, @Value("${quickcourt.export.dir:exports}") String exportDir) {
        this.bookingRepository = bookingRepository;
        this.venueRepository = venueRepository;
        this.courtRepository = courtRepository;
        this.blockedSlotRepository = blockedSlotRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.fileLogger = fileLogger;
        this.exportDir = exportDir;
    }

    // ---------------- availability ----------------

    /** All hourly slots of a court on a date, each marked available or not (with the reason). */
    public List<SlotDto> getSlots(Long venueId, Long courtId, LocalDate date) {
        Venue venue = venueRepository.findById(venueId).orElseThrow(() -> new VenueNotFoundException(venueId));
        if (venue.getStatus() != VenueStatus.APPROVED) {
            throw new VenueNotFoundException(venueId);
        }
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id " + courtId));
        if (!court.getVenue().getId().equals(venueId)) {
            throw new InvalidBookingException("This court does not belong to the selected venue");
        }
        if (date == null || date.isBefore(TimeUtil.today())) {
            throw new InvalidBookingException("Please choose today or a future date");
        }

        Set<String> booked = bookingRepository.findActiveForCourtAndDate(courtId, date, OCCUPYING).stream()
                .map(Booking::getTimeSlot).collect(Collectors.toSet());
        Set<String> blocked = blockedSlotRepository.findForCourtAndDate(courtId, date).stream()
                .map(BlockedSlot::getTimeSlot).collect(Collectors.toSet());

        List<SlotDto> result = new ArrayList<>();
        for (String slot : SlotUtil.generateSlots(venue.getOpeningTime(), venue.getClosingTime())) {
            if (!SlotUtil.startDateTime(date, slot).isAfter(TimeUtil.now())) {
                result.add(new SlotDto(slot, false, "Time passed"));
            } else if (blocked.contains(slot)) {
                result.add(new SlotDto(slot, false, "Maintenance"));
            } else if (booked.contains(slot)) {
                result.add(new SlotDto(slot, false, "Booked"));
            } else {
                result.add(new SlotDto(slot, true, ""));
            }
        }
        return result;
    }

    // ---------------- booking ----------------

    public synchronized Booking createBooking(User user, BookingRequest req) {
        if (user.getRole() != Role.PLAYER) {
            throw new ForbiddenException("Only players can book courts");
        }
        Venue venue = venueRepository.findById(req.venueId())
                .orElseThrow(() -> new VenueNotFoundException(req.venueId()));
        if (venue.getStatus() != VenueStatus.APPROVED) {
            throw new InvalidBookingException("This venue is not open for booking");
        }
        Court court = courtRepository.findById(req.courtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id " + req.courtId()));
        if (!court.getVenue().getId().equals(venue.getId()) || !court.isActive()) {
            throw new InvalidBookingException("Selected court is not available at this venue");
        }

        validateDateAndSlot(venue, req.bookingDate(), req.timeSlot());

        if (blockedSlotRepository.existsBlocked(court.getId(), req.bookingDate(), req.timeSlot())) {
            throw new SlotUnavailableException("This slot is blocked for maintenance");
        }
        if (bookingRepository.existsActiveBooking(court.getId(), req.bookingDate(), req.timeSlot(), OCCUPYING)) {
            throw new SlotUnavailableException("This slot is already booked. Please choose another slot.");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVenue(venue);
        booking.setCourt(court);
        booking.setBookingDate(req.bookingDate());
        booking.setTimeSlot(req.timeSlot());
        booking.setAmount(venue.getPricePerHour()); // 1 hour slot = hourly price
        booking.setBookingStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(booking);
        fileLogger.info("Booking #" + saved.getId() + " created (PENDING) by " + user.getEmail() + " for "
                + venue.getName() + " " + req.bookingDate() + " " + req.timeSlot());
        return saved;
    }

    private void validateDateAndSlot(Venue venue, LocalDate date, String slot) {
        LocalDate today = TimeUtil.today();
        if (date == null || date.isBefore(today)) {
            throw new InvalidBookingException("Booking date cannot be in the past");
        }
        if (date.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new InvalidBookingException("You can book at most " + MAX_ADVANCE_DAYS + " days in advance");
        }
        if (!SlotUtil.isValidFormat(slot)
                || !SlotUtil.generateSlots(venue.getOpeningTime(), venue.getClosingTime()).contains(slot)) {
            throw new InvalidBookingException("Invalid time slot for this venue");
        }
        if (!SlotUtil.startDateTime(date, slot).isAfter(TimeUtil.now())) {
            throw new InvalidBookingException("This time slot has already started or passed");
        }
    }

    public Booking cancelBooking(Long bookingId, User user) {
        Booking b = findBooking(bookingId);
        boolean isOwnerOfBooking = b.getUser().getId().equals(user.getId());
        if (!isOwnerOfBooking && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You can only cancel your own bookings");
        }
        if (b.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("This booking is already cancelled");
        }
        if (!SlotUtil.startDateTime(b.getBookingDate(), b.getTimeSlot()).isAfter(TimeUtil.now())) {
            throw new InvalidBookingException("Past or ongoing bookings cannot be cancelled");
        }
        b.setBookingStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(b);

        paymentRepository.findLatestForBooking(bookingId).ifPresent(p -> {
            if (p.getPaymentStatus() == PaymentStatus.SUCCESS) {
                fileLogger.info("Refund of INR " + p.getAmount() + " simulated for booking #" + bookingId);
            }
        });
        fileLogger.info("Booking #" + bookingId + " cancelled by " + user.getEmail());
        notificationService.sendCancellation(saved);
        return saved;
    }

    // ---------------- reading bookings ----------------

    public Booking getBookingFor(Long bookingId, User user) {
        Booking b = findBooking(bookingId);
        boolean allowed = user.getRole() == Role.ADMIN
                || b.getUser().getId().equals(user.getId())
                || b.getVenue().getOwner().getId().equals(user.getId());
        if (!allowed) {
            throw new ForbiddenException("You cannot view this booking");
        }
        return b;
    }

    public Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
    }

    public List<Booking> getMyBookings(Long userId, BookingStatus statusFilter) {
        return filter(markCompleted(bookingRepository.findByUser(userId)), statusFilter);
    }

    public List<Booking> getOwnerBookings(Long ownerId) {
        return markCompleted(bookingRepository.findByOwner(ownerId));
    }

    public List<Booking> getAllBookings() {
        return markCompleted(bookingRepository.findAllSorted());
    }

    public List<Booking> getBookingsOfUser(Long userId) {
        return markCompleted(bookingRepository.findByUser(userId));
    }

    private List<Booking> filter(List<Booking> list, BookingStatus status) {
        if (status == null) {
            return list;
        }
        return list.stream().filter(b -> b.getBookingStatus() == status).collect(Collectors.toList());
    }

    /** A CONFIRMED booking whose end time has passed becomes COMPLETED. */
    public List<Booking> markCompleted(List<Booking> bookings) {
        LocalDateTime now = TimeUtil.now();
        List<Booking> changed = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getBookingStatus() == BookingStatus.CONFIRMED
                    && !SlotUtil.endDateTime(b.getBookingDate(), b.getTimeSlot()).isAfter(now)) {
                b.setBookingStatus(BookingStatus.COMPLETED);
                changed.add(b);
            }
        }
        if (!changed.isEmpty()) {
            bookingRepository.saveAll(changed);
        }
        return bookings;
    }

    // ---------------- CSV export (file handling) ----------------

    /** Players export their own bookings, owners the bookings of their venues, admin everything. */
    public String exportCsv(User user) {
        List<Booking> list;
        if (user.getRole() == Role.ADMIN) {
            list = getAllBookings();
        } else if (user.getRole() == Role.VENUE_OWNER) {
            list = getOwnerBookings(user.getId());
        } else {
            list = getMyBookings(user.getId(), null);
        }
        String csv = CsvExporter.toCsv(list);
        try {
            CsvExporter.saveToFile(csv, exportDir, user.getRole().name().toLowerCase() + "_bookings");
        } catch (IOException e) {
            fileLogger.warn("Could not save CSV copy: " + e.getMessage());
        }
        fileLogger.info("CSV export by " + user.getEmail() + " (" + list.size() + " rows)");
        return csv;
    }
}
