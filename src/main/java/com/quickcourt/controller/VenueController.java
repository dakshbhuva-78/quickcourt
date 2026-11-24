package com.quickcourt.controller;

import com.quickcourt.dto.*;
import com.quickcourt.model.*;
import com.quickcourt.service.BookingService;
import com.quickcourt.service.StatsService;
import com.quickcourt.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Public venue browsing (/api/venues/**) and the venue OWNER area (/api/owner/**).
 * The AuthInterceptor already guarantees that /api/owner/** is only reachable by VENUE_OWNER users.
 */
@RestController
@RequestMapping("/api")
public class VenueController {

    private final VenueService venueService;
    private final BookingService bookingService;
    private final StatsService statsService;

    public VenueController(VenueService venueService, BookingService bookingService, StatsService statsService) {
        this.venueService = venueService;
        this.bookingService = bookingService;
        this.statsService = statsService;
    }

    // ---------------- public ----------------

    @GetMapping("/venues")
    public List<Venue> search(@RequestParam(required = false) SportType sport,
                              @RequestParam(required = false) String location,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) BigDecimal maxPrice) {
        return venueService.search(sport, location, q, maxPrice);
    }

    @GetMapping("/venues/sports")
    public SportType[] sports() {
        return SportType.values();
    }

    @GetMapping("/venues/{id}")
    public Venue details(@PathVariable Long id) {
        return venueService.getApprovedVenue(id);
    }

    @GetMapping("/venues/{id}/courts")
    public List<Court> courts(@PathVariable Long id) {
        venueService.getApprovedVenue(id);
        return venueService.getCourts(id, true);
    }

    @GetMapping("/venues/{id}/slots")
    public List<SlotDto> slots(@PathVariable Long id, @RequestParam Long courtId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.getSlots(id, courtId, date);
    }

    // ---------------- owner ----------------

    @GetMapping("/owner/venues")
    public List<Venue> myVenues(@RequestAttribute("currentUser") User owner) {
        return venueService.listByOwner(owner.getId());
    }

    @PostMapping("/owner/venues")
    @ResponseStatus(HttpStatus.CREATED)
    public Venue addVenue(@RequestAttribute("currentUser") User owner, @Valid @RequestBody VenueRequest request) {
        return venueService.createVenue(owner, request);
    }

    @PutMapping("/owner/venues/{id}")
    public Venue editVenue(@RequestAttribute("currentUser") User owner, @PathVariable Long id,
                           @Valid @RequestBody VenueRequest request) {
        return venueService.updateVenue(owner, id, request);
    }

    @GetMapping("/owner/venues/{id}/courts")
    public List<Court> ownerCourts(@RequestAttribute("currentUser") User owner, @PathVariable Long id) {
        Venue v = venueService.getVenue(id);
        if (!v.getOwner().getId().equals(owner.getId())) {
            throw new com.quickcourt.exception.ForbiddenException("You do not own this venue");
        }
        return venueService.getCourts(id, true);
    }

    @PostMapping("/owner/venues/{id}/courts")
    @ResponseStatus(HttpStatus.CREATED)
    public Court addCourt(@RequestAttribute("currentUser") User owner, @PathVariable Long id,
                          @Valid @RequestBody CourtRequest request) {
        return venueService.addCourt(owner, id, request);
    }

    @PutMapping("/owner/courts/{courtId}")
    public Court editCourt(@RequestAttribute("currentUser") User owner, @PathVariable Long courtId,
                           @Valid @RequestBody CourtRequest request) {
        return venueService.updateCourt(owner, courtId, request);
    }

    @DeleteMapping("/owner/courts/{courtId}")
    public Map<String, String> deleteCourt(@RequestAttribute("currentUser") User owner, @PathVariable Long courtId) {
        venueService.deactivateCourt(owner, courtId);
        return Map.of("message", "Court removed");
    }

    @GetMapping("/owner/courts/{courtId}/blocks")
    public List<BlockedSlot> blocks(@RequestAttribute("currentUser") User owner, @PathVariable Long courtId) {
        return venueService.listBlocks(owner, courtId);
    }

    @PostMapping("/owner/courts/{courtId}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public BlockedSlot block(@RequestAttribute("currentUser") User owner, @PathVariable Long courtId,
                             @Valid @RequestBody BlockSlotRequest request) {
        return venueService.blockSlot(owner, courtId, request);
    }

    @DeleteMapping("/owner/blocks/{blockId}")
    public Map<String, String> unblock(@RequestAttribute("currentUser") User owner, @PathVariable Long blockId) {
        venueService.unblockSlot(owner, blockId);
        return Map.of("message", "Slot unblocked");
    }

    @GetMapping("/owner/bookings")
    public List<Booking> ownerBookings(@RequestAttribute("currentUser") User owner) {
        return bookingService.getOwnerBookings(owner.getId());
    }

    @GetMapping("/owner/stats")
    public StatsResponse ownerStats(@RequestAttribute("currentUser") User owner) {
        return statsService.forOwner(owner.getId());
    }
}
