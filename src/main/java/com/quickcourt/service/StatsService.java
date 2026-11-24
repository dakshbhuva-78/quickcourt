package com.quickcourt.service;

import com.quickcourt.dto.StatsResponse;
import com.quickcourt.model.*;
import com.quickcourt.repository.CourtRepository;
import com.quickcourt.repository.UserRepository;
import com.quickcourt.repository.VenueRepository;
import com.quickcourt.util.SlotUtil;
import com.quickcourt.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Calculates the numbers shown on the owner and admin dashboards. */
@Service
public class StatsService {

    private final BookingService bookingService;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    public StatsService(BookingService bookingService, VenueRepository venueRepository,
                        CourtRepository courtRepository, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.venueRepository = venueRepository;
        this.courtRepository = courtRepository;
        this.userRepository = userRepository;
    }

    public StatsResponse forOwner(Long ownerId) {
        List<Booking> bookings = bookingService.getOwnerBookings(ownerId);
        List<Venue> venues = venueRepository.findByOwnerId(ownerId);
        long pending = venues.stream().filter(v -> v.getStatus() == VenueStatus.PENDING).count();
        return build(bookings, courtRepository.countActiveByOwner(ownerId), venues.size(), pending,
                0, 0, new LinkedHashMap<>(), countByStatus(venues));
    }

    public StatsResponse forAdmin() {
        List<Booking> bookings = bookingService.getAllBookings();
        List<Venue> venues = venueRepository.findAll();
        long pending = venues.stream().filter(v -> v.getStatus() == VenueStatus.PENDING).count();

        Map<String, Long> registrations = new LinkedHashMap<>();
        List<User> users = userRepository.findAll();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = TimeUtil.today().minusDays(i);
            long count = users.stream()
                    .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(day)).count();
            registrations.put(day.toString(), count);
        }
        return build(bookings, courtRepository.countByActiveTrue(), venues.size(), pending,
                users.size(), userRepository.countByRole(Role.VENUE_OWNER), registrations, countByStatus(venues));
    }

    private Map<String, Long> countByStatus(List<Venue> venues) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (VenueStatus s : VenueStatus.values()) {
            map.put(s.name(), venues.stream().filter(v -> v.getStatus() == s).count());
        }
        return map;
    }

    private StatsResponse build(List<Booking> bookings, long activeCourts, long totalVenues, long pendingVenues,
                                long totalUsers, long totalOwners, Map<String, Long> registrations,
                                Map<String, Long> venuesByStatus) {
        // "paid" = bookings that really happen (confirmed or completed)
        List<Booking> paid = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED || b.getBookingStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.toList());
        long cancelled = bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count();
        BigDecimal earnings = paid.stream().map(Booking::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3 days back .. 3 days ahead
        Map<String, Long> byDay = new LinkedHashMap<>();
        for (int i = -3; i <= 3; i++) {
            LocalDate day = TimeUtil.today().plusDays(i);
            byDay.put(day.toString(), paid.stream().filter(b -> b.getBookingDate().equals(day)).count());
        }
        Map<String, Long> bySport = paid.stream().collect(Collectors.groupingBy(
                b -> b.getVenue().getSportType().name(), TreeMap::new, Collectors.counting()));
        Map<String, Long> byHour = paid.stream().collect(Collectors.groupingBy(
                b -> SlotUtil.startOf(b.getTimeSlot()).toString(), TreeMap::new, Collectors.counting()));
        Map<String, BigDecimal> earningsBySport = paid.stream().collect(Collectors.groupingBy(
                b -> b.getVenue().getSportType().name(), TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, Booking::getAmount, BigDecimal::add)));

        return new StatsResponse(bookings.size(), paid.size(), cancelled, activeCourts, totalVenues, pendingVenues,
                totalUsers, totalOwners, earnings, byDay, bySport, byHour, earningsBySport, registrations, venuesByStatus);
    }
}
