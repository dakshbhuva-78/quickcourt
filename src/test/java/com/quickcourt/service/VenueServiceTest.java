package com.quickcourt.service;

import com.quickcourt.dto.BlockSlotRequest;
import com.quickcourt.dto.VenueRequest;
import com.quickcourt.exception.*;
import com.quickcourt.model.*;
import com.quickcourt.repository.*;
import com.quickcourt.util.FileLogger;
import com.quickcourt.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit tests for venue search, owner actions and admin approval. */
class VenueServiceTest {

    private VenueRepository venueRepository;
    private CourtRepository courtRepository;
    private BlockedSlotRepository blockedSlotRepository;
    private BookingRepository bookingRepository;
    private VenueService service;
    private VenueOwner owner;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        venueRepository = mock(VenueRepository.class);
        courtRepository = mock(CourtRepository.class);
        blockedSlotRepository = mock(BlockedSlotRepository.class);
        bookingRepository = mock(BookingRepository.class);
        service = new VenueService(venueRepository, courtRepository, blockedSlotRepository, bookingRepository,
                new FileLogger(tmp.toString()));
        owner = new VenueOwner("Rahul", "r@mail.com", "hash");
        owner.setId(1L);
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Venue venue(Long id, String name, String location, SportType sport, String price, VenueStatus status) {
        Venue v = new Venue();
        v.setId(id);
        v.setName(name);
        v.setLocation(location);
        v.setSportType(sport);
        v.setPricePerHour(new BigDecimal(price));
        v.setOpeningTime(LocalTime.of(6, 0));
        v.setClosingTime(LocalTime.of(22, 0));
        v.setStatus(status);
        v.setOwner(owner);
        return v;
    }

    private VenueRequest request(LocalTime open, LocalTime close, String imageUrl) {
        return new VenueRequest("New Venue", "Bopal", "desc", SportType.TENNIS, new BigDecimal("500"), open, close, "Parking", imageUrl);
    }

    private void stubApprovedList() {
        when(venueRepository.findByStatus(VenueStatus.APPROVED)).thenReturn(List.of(
                venue(1L, "Green Arena", "Satellite, Ahmedabad", SportType.BADMINTON, "400", VenueStatus.APPROVED),
                venue(2L, "Turf Kings", "Bopal, Ahmedabad", SportType.FOOTBALL, "1200", VenueStatus.APPROVED),
                venue(3L, "Ace Courts", "Satellite, Ahmedabad", SportType.BADMINTON, "300", VenueStatus.APPROVED)));
    }

    // ---------- search ----------

    @Test
    void searchWithoutFiltersReturnsAllSortedByName() {
        stubApprovedList();
        List<Venue> r = service.search(null, null, null, null);
        assertEquals(3, r.size());
        assertEquals("Ace Courts", r.get(0).getName());
    }

    @Test
    void searchBySport() {
        stubApprovedList();
        assertEquals(2, service.search(SportType.BADMINTON, null, null, null).size());
    }

    @Test
    void searchByLocationIgnoresCase() {
        stubApprovedList();
        assertEquals(1, service.search(null, "BOPAL", null, null).size());
    }

    @Test
    void searchByMaxPriceIncludesBoundary() {
        stubApprovedList();
        assertEquals(2, service.search(null, null, null, new BigDecimal("400")).size());
    }

    @Test
    void searchByNameAndCombinedFilters() {
        stubApprovedList();
        assertEquals(1, service.search(SportType.BADMINTON, "satellite", "green", new BigDecimal("500")).size());
    }

    @Test
    void searchWithNoMatchReturnsEmptyList() {
        stubApprovedList();
        assertTrue(service.search(SportType.CRICKET, null, null, null).isEmpty());
    }

    // ---------- details ----------

    @Test
    void pendingVenueIsHiddenFromPlayers() {
        when(venueRepository.findById(5L)).thenReturn(Optional.of(venue(5L, "X", "Y", SportType.TENNIS, "100", VenueStatus.PENDING)));
        assertThrows(VenueNotFoundException.class, () -> service.getApprovedVenue(5L));
    }

    @Test
    void unknownVenueThrows() {
        when(venueRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(VenueNotFoundException.class, () -> service.getApprovedVenue(404L));
    }

    // ---------- owner ----------

    @Test
    void newVenueIsPending() {
        Venue v = service.createVenue(owner, request(LocalTime.of(6, 0), LocalTime.of(22, 0), null));
        assertEquals(VenueStatus.PENDING, v.getStatus());
        assertEquals(owner, v.getOwner());
    }

    @Test
    void closingTimeBeforeOpeningIsRejected() {
        assertThrows(InvalidRequestException.class,
                () -> service.createVenue(owner, request(LocalTime.of(22, 0), LocalTime.of(6, 0), null)));
    }

    @Test
    void unsafeImageLinkIsDropped() {
        Venue v = service.createVenue(owner, request(LocalTime.of(6, 0), LocalTime.of(22, 0), "javascript:alert(1)"));
        assertNull(v.getImageUrl());
        Venue ok = service.createVenue(owner, request(LocalTime.of(6, 0), LocalTime.of(22, 0), "https://example.com/a.jpg"));
        assertEquals("https://example.com/a.jpg", ok.getImageUrl());
    }

    @Test
    void ownerCannotEditSomeoneElsesVenue() {
        VenueOwner other = new VenueOwner("Other", "o@mail.com", "hash");
        other.setId(99L);
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue(1L, "A", "B", SportType.TENNIS, "100", VenueStatus.APPROVED)));
        assertThrows(ForbiddenException.class,
                () -> service.updateVenue(other, 1L, request(LocalTime.of(6, 0), LocalTime.of(22, 0), null)));
    }

    @Test
    void editingRejectedVenueResubmitsIt() {
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue(1L, "A", "B", SportType.TENNIS, "100", VenueStatus.REJECTED)));
        Venue v = service.updateVenue(owner, 1L, request(LocalTime.of(6, 0), LocalTime.of(22, 0), null));
        assertEquals(VenueStatus.PENDING, v.getStatus());
    }

    // ---------- blocked slots ----------

    private Court courtOfOwner() {
        Court c = new Court();
        c.setId(7L);
        c.setVenue(venue(1L, "A", "B", SportType.TENNIS, "100", VenueStatus.APPROVED));
        when(courtRepository.findById(7L)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void ownerCanBlockFreeSlot() {
        courtOfOwner();
        when(blockedSlotRepository.save(any(BlockedSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        BlockedSlot b = service.blockSlot(owner, 7L, new BlockSlotRequest(TimeUtil.today().plusDays(2), "10:00-11:00", ""));
        assertEquals("Maintenance", b.getReason());
    }

    @Test
    void cannotBlockSlotInThePast() {
        courtOfOwner();
        assertThrows(InvalidRequestException.class,
                () -> service.blockSlot(owner, 7L, new BlockSlotRequest(TimeUtil.today().minusDays(1), "10:00-11:00", "x")));
    }

    @Test
    void cannotBlockSlotThatIsAlreadyBooked() {
        courtOfOwner();
        when(bookingRepository.existsActiveBooking(eq(7L), any(), eq("10:00-11:00"), any())).thenReturn(true);
        assertThrows(SlotUnavailableException.class,
                () -> service.blockSlot(owner, 7L, new BlockSlotRequest(TimeUtil.today().plusDays(2), "10:00-11:00", "x")));
    }

    // ---------- admin ----------

    @Test
    void adminApprovesVenueWithComment() {
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue(1L, "A", "B", SportType.TENNIS, "100", VenueStatus.PENDING)));
        Venue v = service.review(1L, true, "Looks good");
        assertEquals(VenueStatus.APPROVED, v.getStatus());
        assertEquals("Looks good", v.getAdminComment());
    }

    @Test
    void adminRejectsVenue() {
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue(1L, "A", "B", SportType.TENNIS, "100", VenueStatus.PENDING)));
        assertEquals(VenueStatus.REJECTED, service.review(1L, false, " ").getStatus());
    }
}
