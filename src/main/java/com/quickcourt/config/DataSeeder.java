package com.quickcourt.config;

import com.quickcourt.model.*;
import com.quickcourt.repository.*;
import com.quickcourt.util.TimeUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Inserts SAMPLE DATA the first time the app starts (only if the users table is empty).
 * Demo logins (all fake):
 *   admin@quickcourt.com   / Admin@123
 *   owner1@quickcourt.com  / Owner@123     owner2@quickcourt.com / Owner@123
 *   player1@quickcourt.com / Player@123    player2@quickcourt.com / Player@123
 * Turn off with SEED_DATA=false.
 */
@Component
@ConditionalOnProperty(name = "quickcourt.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository userRepository, VenueRepository venueRepository, CourtRepository courtRepository,
                      BookingRepository bookingRepository, PaymentRepository paymentRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // ---- users ----
        userRepository.save(aged(new Admin("QuickCourt Admin", "admin@quickcourt.com", encoder.encode("Admin@123")), 10));
        User owner1 = userRepository.save(aged(new VenueOwner("Rahul Patel", "owner1@quickcourt.com", encoder.encode("Owner@123")), 8));
        User owner2 = userRepository.save(aged(new VenueOwner("Sneha Shah", "owner2@quickcourt.com", encoder.encode("Owner@123")), 6));
        User player1 = userRepository.save(aged(new Player("Aarav Mehta", "player1@quickcourt.com", encoder.encode("Player@123")), 5));
        User player2 = userRepository.save(aged(new Player("Diya Desai", "player2@quickcourt.com", encoder.encode("Player@123")), 3));
        userRepository.save(aged(new Player("Kabir Joshi", "player3@quickcourt.com", encoder.encode("Player@123")), 1));

        // ---- venues + courts ----
        Venue v1 = venue(owner1, "Green Arena Badminton Club", "Satellite, Ahmedabad", SportType.BADMINTON, "400",
                6, 22, "Parking,Changing Room,Drinking Water,Wooden Flooring",
                "Six synthetic-flooring badminton courts with bright LED lighting, ideal for singles and doubles.", VenueStatus.APPROVED);
        Court c1a = court(v1, "Court 1"); court(v1, "Court 2"); court(v1, "Court 3");

        Venue v2 = venue(owner1, "Turf Kings Football Ground", "Bopal, Ahmedabad", SportType.FOOTBALL, "1200",
                6, 23, "Floodlights,Parking,Washroom,Cafe",
                "5-a-side and 7-a-side artificial turf grounds with floodlights for night matches.", VenueStatus.APPROVED);
        Court c2a = court(v2, "Turf A"); court(v2, "Turf B");

        Venue v3 = venue(owner1, "Smash Tennis Academy", "Prahlad Nagar, Ahmedabad", SportType.TENNIS, "600",
                6, 21, "Coaching,Parking,Drinking Water,Equipment Rental",
                "Hard-court tennis with certified coaches and racket rental.", VenueStatus.APPROVED);
        court(v3, "Court A"); court(v3, "Court B");

        Venue v4 = venue(owner1, "Pro Cricket Nets", "Thaltej, Ahmedabad", SportType.CRICKET, "800",
                7, 22, "Bowling Machine,Floodlights,Parking",
                "Practice nets with turf and cement pitches. Bowling machine available.", VenueStatus.APPROVED);
        court(v4, "Net 1"); court(v4, "Net 2"); court(v4, "Net 3");

        Venue v5 = venue(owner2, "Table Tennis Hub", "Navrangpura, Ahmedabad", SportType.TABLE_TENNIS, "250",
                9, 21, "Air Conditioned,Equipment Rental,Cafe",
                "Air-conditioned hall with tournament-grade tables.", VenueStatus.APPROVED);
        court(v5, "Table 1"); court(v5, "Table 2");

        Venue v6 = venue(owner2, "City Hoops Basketball Court", "Vastrapur, Ahmedabad", SportType.BASKETBALL, "500",
                6, 22, "Floodlights,Washroom,Parking",
                "Full-size outdoor basketball court with night lighting.", VenueStatus.APPROVED);
        court(v6, "Full Court");

        Venue v7 = venue(owner2, "Ace Badminton Studio", "Maninagar, Ahmedabad", SportType.BADMINTON, "350",
                6, 22, "Parking,Drinking Water",
                "New 2-court badminton studio. Waiting for admin approval.", VenueStatus.PENDING);
        court(v7, "Court 1");

        // ---- sample bookings (some past, some future) ----
        book(player1, v1, c1a, -3, "18:00-19:00", BookingStatus.CONFIRMED);
        book(player2, v1, c1a, -2, "19:00-20:00", BookingStatus.CONFIRMED);
        book(player1, v2, c2a, -1, "20:00-21:00", BookingStatus.CONFIRMED);
        book(player2, v2, c2a, 1, "18:00-19:00", BookingStatus.CONFIRMED);
        book(player1, v1, c1a, 2, "07:00-08:00", BookingStatus.CONFIRMED);
        book(player2, v1, c1a, 2, "18:00-19:00", BookingStatus.CONFIRMED);
        book(player1, v2, c2a, 3, "21:00-22:00", BookingStatus.CANCELLED);
    }

    // ----- small helpers to keep run() readable -----
    private User aged(User u, int daysAgo) {
        u.setCreatedAt(TimeUtil.now().minusDays(daysAgo));
        return u;
    }

    private Venue venue(User owner, String name, String location, SportType sport, String price,
                        int open, int close, String amenities, String description, VenueStatus status) {
        Venue v = new Venue();
        v.setOwner(owner);
        v.setName(name);
        v.setLocation(location);
        v.setSportType(sport);
        v.setPricePerHour(new BigDecimal(price));
        v.setOpeningTime(LocalTime.of(open, 0));
        v.setClosingTime(LocalTime.of(close, 0));
        v.setAmenities(amenities);
        v.setDescription(description);
        v.setStatus(status);
        return venueRepository.save(v);
    }

    private Court court(Venue venue, String name) {
        Court c = new Court();
        c.setVenue(venue);
        c.setCourtName(name);
        c.setSportType(venue.getSportType());
        return courtRepository.save(c);
    }

    private void book(User player, Venue venue, Court court, int dayOffset, String slot, BookingStatus status) {
        Booking b = new Booking();
        b.setUser(player);
        b.setVenue(venue);
        b.setCourt(court);
        b.setBookingDate(TimeUtil.today().plusDays(dayOffset));
        b.setTimeSlot(slot);
        b.setAmount(venue.getPricePerHour());
        b.setBookingStatus(status);
        b = bookingRepository.save(b);

        Payment p = new Payment();
        p.setBooking(b);
        p.setAmount(b.getAmount());
        p.setPaymentMethod(PaymentMethod.UPI);
        p.setPaymentStatus(PaymentStatus.SUCCESS);
        p.setTransactionReference("QC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paymentRepository.save(p);
    }
}
