package com.quickcourt.integration;

import com.jayway.jsonpath.JsonPath;
import com.quickcourt.model.*;
import com.quickcourt.repository.CourtRepository;
import com.quickcourt.repository.UserRepository;
import com.quickcourt.repository.VenueRepository;
import com.quickcourt.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * INTEGRATION TESTS: start the whole Spring Boot app (with an in-memory H2 database)
 * and call the real REST API with MockMvc: controller -> service -> repository -> database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuickCourtIntegrationTest {

    private static final String PASSWORD = "Strong@123";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired VenueRepository venueRepository;
    @Autowired CourtRepository courtRepository;
    @Autowired PasswordEncoder encoder;

    // ---------------- helpers ----------------

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private ResultActions doPost(String url, String token, String json) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    private ResultActions doPut(String url, String token, String json) throws Exception {
        var req = put(url).contentType(MediaType.APPLICATION_JSON).content(json == null ? "{}" : json);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    private ResultActions doGet(String url, String token) throws Exception {
        var req = get(url);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    private String registerAndLogin(String email, Role role) throws Exception {
        doPost("/api/auth/register", null, String.format(
                "{\"name\":\"Test User\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", email, PASSWORD, role))
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    private String login(String email, String password) throws Exception {
        String body = doPost("/api/auth/login", null, String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String adminToken() throws Exception {
        String email = uniqueEmail("admin");
        userRepository.save(new Admin("Admin", email, encoder.encode(PASSWORD)));
        return login(email, PASSWORD);
    }

    /** Creates an APPROVED venue (06:00-22:00, Rs 500) with one court directly in the database. Returns {venueId, courtId}. */
    private long[] approvedVenue(String name) {
        VenueOwner owner = userRepository.save(new VenueOwner("Owner", uniqueEmail("owner"), encoder.encode(PASSWORD)));
        Venue v = new Venue();
        v.setOwner(owner);
        v.setName(name);
        v.setLocation("Satellite, Ahmedabad");
        v.setSportType(SportType.BADMINTON);
        v.setPricePerHour(new BigDecimal("500"));
        v.setOpeningTime(LocalTime.of(6, 0));
        v.setClosingTime(LocalTime.of(22, 0));
        v.setStatus(VenueStatus.APPROVED);
        v = venueRepository.save(v);
        Court c = new Court();
        c.setVenue(v);
        c.setCourtName("Court 1");
        c.setSportType(SportType.BADMINTON);
        c = courtRepository.save(c);
        return new long[]{v.getId(), c.getId()};
    }

    private String bookingJson(long venueId, long courtId, String date, String slot) {
        return String.format("{\"venueId\":%d,\"courtId\":%d,\"bookingDate\":\"%s\",\"timeSlot\":\"%s\"}", venueId, courtId, date, slot);
    }

    private String futureDate() {
        return TimeUtil.today().plusDays(2).toString();
    }

    // ---------------- authentication ----------------

    @Test
    void registerThenLoginSucceeds() throws Exception {
        String email = uniqueEmail("player");
        registerAndLogin(email, Role.PLAYER);
    }

    @Test
    void duplicateEmailGivesConflict() throws Exception {
        String email = uniqueEmail("dup");
        registerAndLogin(email, Role.PLAYER);
        doPost("/api/auth/register", null, String.format(
                "{\"name\":\"Again\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"PLAYER\"}", email, PASSWORD))
                .andExpect(status().isConflict());
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        doPost("/api/auth/register", null, String.format(
                "{\"name\":\"Weak\",\"email\":\"%s\",\"password\":\"abc\",\"role\":\"PLAYER\"}", uniqueEmail("weak")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordGivesUnauthorized() throws Exception {
        String email = uniqueEmail("wrong");
        registerAndLogin(email, Role.PLAYER);
        doPost("/api/auth/login", null, String.format("{\"email\":\"%s\",\"password\":\"Nope@12345\"}", email))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordHashIsNeverReturned() throws Exception {
        String token = registerAndLogin(uniqueEmail("me"), Role.PLAYER);
        String body = doGet("/api/users/me", token).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("password"));
    }

    // ---------------- access control ----------------

    @Test
    void bookingWithoutLoginIsUnauthorized() throws Exception {
        long[] ids = approvedVenue("Venue " + UUID.randomUUID());
        doPost("/api/bookings", null, bookingJson(ids[0], ids[1], futureDate(), "18:00-19:00")).andExpect(status().isUnauthorized());
    }

    @Test
    void playerCannotOpenAdminApi() throws Exception {
        String token = registerAndLogin(uniqueEmail("p"), Role.PLAYER);
        doGet("/api/admin/stats", token).andExpect(status().isForbidden());
    }

    @Test
    void playerCannotOpenOwnerApi() throws Exception {
        String token = registerAndLogin(uniqueEmail("p"), Role.PLAYER);
        doGet("/api/owner/venues", token).andExpect(status().isForbidden());
    }

    // ---------------- venue search ----------------

    @Test
    void searchFindsApprovedVenueBySportAndLocation() throws Exception {
        String name = "SearchVenue-" + UUID.randomUUID().toString().substring(0, 6);
        approvedVenue(name);
        doGet("/api/venues?sport=BADMINTON&location=satellite&q=" + name, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(name));
    }

    @Test
    void searchWithUnknownNameReturnsEmptyList() throws Exception {
        doGet("/api/venues?q=zzz-no-such-venue-zzz", null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // ---------------- booking flow ----------------

    @Test
    void fullBookingFlow_book_pay_list_cancel() throws Exception {
        long[] ids = approvedVenue("Flow " + UUID.randomUUID());
        String token = registerAndLogin(uniqueEmail("flow"), Role.PLAYER);

        String created = doPost("/api/bookings", token, bookingJson(ids[0], ids[1], futureDate(), "18:00-19:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(500.0))
                .andReturn().getResponse().getContentAsString();
        int bookingId = JsonPath.read(created, "$.id");

        doPost("/api/payments", token, String.format(
                "{\"bookingId\":%d,\"paymentMethod\":\"UPI\",\"upiId\":\"demo@upi\"}", bookingId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        doGet("/api/bookings/my", token).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingStatus").value("CONFIRMED"));

        doPut("/api/bookings/" + bookingId + "/cancel", token, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));
    }

    @Test
    void duplicateSlotBookingIsRejected() throws Exception {
        long[] ids = approvedVenue("Dup " + UUID.randomUUID());
        String t1 = registerAndLogin(uniqueEmail("a"), Role.PLAYER);
        String t2 = registerAndLogin(uniqueEmail("b"), Role.PLAYER);
        String json = bookingJson(ids[0], ids[1], futureDate(), "19:00-20:00");
        doPost("/api/bookings", t1, json).andExpect(status().isCreated());
        doPost("/api/bookings", t2, json).andExpect(status().isConflict());
    }

    @Test
    void cancelledSlotCanBeBookedAgain() throws Exception {
        long[] ids = approvedVenue("Rebook " + UUID.randomUUID());
        String t1 = registerAndLogin(uniqueEmail("a"), Role.PLAYER);
        String t2 = registerAndLogin(uniqueEmail("b"), Role.PLAYER);
        String json = bookingJson(ids[0], ids[1], futureDate(), "20:00-21:00");
        String body = doPost("/api/bookings", t1, json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");
        doPut("/api/bookings/" + id + "/cancel", t1, null).andExpect(status().isOk());
        doPost("/api/bookings", t2, json).andExpect(status().isCreated());
    }

    @Test
    void pastDateIsRejected() throws Exception {
        long[] ids = approvedVenue("Past " + UUID.randomUUID());
        String token = registerAndLogin(uniqueEmail("past"), Role.PLAYER);
        doPost("/api/bookings", token, bookingJson(ids[0], ids[1], TimeUtil.today().minusDays(1).toString(), "18:00-19:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookedSlotShowsAsUnavailableInSlotList() throws Exception {
        long[] ids = approvedVenue("Slots " + UUID.randomUUID());
        String token = registerAndLogin(uniqueEmail("slot"), Role.PLAYER);
        doPost("/api/bookings", token, bookingJson(ids[0], ids[1], futureDate(), "17:00-18:00")).andExpect(status().isCreated());
        doGet("/api/venues/" + ids[0] + "/slots?courtId=" + ids[1] + "&date=" + futureDate(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.timeSlot=='17:00-18:00')].available").value(false))
                .andExpect(jsonPath("$[?(@.timeSlot=='18:00-19:00')].available").value(true));
    }

    // ---------------- payment ----------------

    @Test
    void failedPaymentKeepsBookingPending() throws Exception {
        long[] ids = approvedVenue("PayFail " + UUID.randomUUID());
        String token = registerAndLogin(uniqueEmail("pf"), Role.PLAYER);
        String body = doPost("/api/bookings", token, bookingJson(ids[0], ids[1], futureDate(), "16:00-17:00"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");

        doPost("/api/payments", token, String.format(
                "{\"bookingId\":%d,\"paymentMethod\":\"CARD\",\"cardNumber\":\"4111111111110000\"}", id))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        doGet("/api/bookings/" + id, token).andExpect(jsonPath("$.bookingStatus").value("PENDING"));

        // retry with a good method succeeds
        doPost("/api/payments", token, String.format("{\"bookingId\":%d,\"paymentMethod\":\"CASH\"}", id))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        doGet("/api/bookings/" + id, token).andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));
    }

    @Test
    void cannotPayForAnotherPlayersBooking() throws Exception {
        long[] ids = approvedVenue("Other " + UUID.randomUUID());
        String t1 = registerAndLogin(uniqueEmail("o1"), Role.PLAYER);
        String t2 = registerAndLogin(uniqueEmail("o2"), Role.PLAYER);
        String body = doPost("/api/bookings", t1, bookingJson(ids[0], ids[1], futureDate(), "15:00-16:00"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");
        doPost("/api/payments", t2, String.format("{\"bookingId\":%d,\"paymentMethod\":\"CASH\"}", id)).andExpect(status().isForbidden());
    }

    // ---------------- owner + admin ----------------

    @Test
    void ownerAddsVenue_adminApproves_playerCanSeeIt() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail("own"), Role.VENUE_OWNER);
        String name = "NewVenue-" + UUID.randomUUID().toString().substring(0, 6);

        String created = doPost("/api/owner/venues", ownerToken, String.format(
                "{\"name\":\"%s\",\"location\":\"Bopal\",\"description\":\"Nice\",\"sportType\":\"TENNIS\","
                        + "\"pricePerHour\":600,\"openingTime\":\"06:00\",\"closingTime\":\"21:00\",\"amenities\":\"Parking\"}", name))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        int venueId = JsonPath.read(created, "$.id");

        // not visible to the public yet
        doGet("/api/venues?q=" + name, null).andExpect(jsonPath("$").isEmpty());

        String admin = adminToken();
        doPut("/api/admin/venues/" + venueId + "/approve", admin, "{\"comment\":\"OK\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));

        doGet("/api/venues?q=" + name, null).andExpect(jsonPath("$[0].name").value(name));
    }

    @Test
    void adminCanRejectVenue() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail("own"), Role.VENUE_OWNER);
        String created = doPost("/api/owner/venues", ownerToken,
                "{\"name\":\"Reject Me\",\"location\":\"X\",\"sportType\":\"CRICKET\",\"pricePerHour\":300,\"openingTime\":\"07:00\",\"closingTime\":\"20:00\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");
        doPut("/api/admin/venues/" + id + "/reject", adminToken(), "{\"comment\":\"Missing photos\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void invalidVenueDataIsRejected() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail("own"), Role.VENUE_OWNER);
        doPost("/api/owner/venues", ownerToken,
                "{\"name\":\"\",\"location\":\"X\",\"sportType\":\"CRICKET\",\"pricePerHour\":300,\"openingTime\":\"07:00\",\"closingTime\":\"20:00\"}")
                .andExpect(status().isBadRequest());
        doPost("/api/owner/venues", ownerToken,
                "{\"name\":\"Bad hours\",\"location\":\"X\",\"sportType\":\"CRICKET\",\"pricePerHour\":300,\"openingTime\":\"20:00\",\"closingTime\":\"07:00\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerBlocksSlot_playerCannotBookIt() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail("own"), Role.VENUE_OWNER);
        String created = doPost("/api/owner/venues", ownerToken,
                "{\"name\":\"Block Venue\",\"location\":\"X\",\"sportType\":\"TENNIS\",\"pricePerHour\":300,\"openingTime\":\"06:00\",\"closingTime\":\"22:00\"}")
                .andReturn().getResponse().getContentAsString();
        int venueId = JsonPath.read(created, "$.id");
        String court = doPost("/api/owner/venues/" + venueId + "/courts", ownerToken, "{\"courtName\":\"Court A\",\"sportType\":\"TENNIS\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int courtId = JsonPath.read(court, "$.id");
        doPut("/api/admin/venues/" + venueId + "/approve", adminToken(), null).andExpect(status().isOk());

        doPost("/api/owner/courts/" + courtId + "/blocks", ownerToken,
                String.format("{\"date\":\"%s\",\"timeSlot\":\"10:00-11:00\",\"reason\":\"Repairs\"}", futureDate()))
                .andExpect(status().isCreated());

        String player = registerAndLogin(uniqueEmail("pl"), Role.PLAYER);
        doPost("/api/bookings", player, bookingJson(venueId, courtId, futureDate(), "10:00-11:00")).andExpect(status().isConflict());
    }

    @Test
    void adminStatsAndOwnerStatsAreAvailable() throws Exception {
        doGet("/api/admin/stats", adminToken()).andExpect(status().isOk()).andExpect(jsonPath("$.totalUsers").exists());
        String ownerToken = registerAndLogin(uniqueEmail("own"), Role.VENUE_OWNER);
        doGet("/api/owner/stats", ownerToken).andExpect(status().isOk()).andExpect(jsonPath("$.totalBookings").value(0));
    }

    @Test
    void csvExportReturnsHeader() throws Exception {
        String token = registerAndLogin(uniqueEmail("csv"), Role.PLAYER);
        doGet("/api/bookings/export", token).andExpect(status().isOk())
                .andExpect(content().string(containsString("BookingId,Player,Venue")));
    }

    @Test
    void bannedUserCannotLogin() throws Exception {
        String email = uniqueEmail("ban");
        registerAndLogin(email, Role.PLAYER);
        long userId = userRepository.findByEmail(email).orElseThrow().getId();
        doPut("/api/admin/users/" + userId + "/ban", adminToken(), null).andExpect(status().isOk());
        doPost("/api/auth/login", null, String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, PASSWORD))
                .andExpect(status().isForbidden());
    }
}
