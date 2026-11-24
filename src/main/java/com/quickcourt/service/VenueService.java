package com.quickcourt.service;

import com.quickcourt.dto.BlockSlotRequest;
import com.quickcourt.dto.CourtRequest;
import com.quickcourt.dto.VenueRequest;
import com.quickcourt.exception.*;
import com.quickcourt.model.*;
import com.quickcourt.repository.BlockedSlotRepository;
import com.quickcourt.repository.BookingRepository;
import com.quickcourt.repository.CourtRepository;
import com.quickcourt.repository.VenueRepository;
import com.quickcourt.util.FileLogger;
import com.quickcourt.util.SlotUtil;
import com.quickcourt.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Venue search, owner venue/court management, blocked slots and admin approval. */
@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final BookingRepository bookingRepository;
    private final FileLogger fileLogger;

    public VenueService(VenueRepository venueRepository, CourtRepository courtRepository,
                        BlockedSlotRepository blockedSlotRepository, BookingRepository bookingRepository,
                        FileLogger fileLogger) {
        this.venueRepository = venueRepository;
        this.courtRepository = courtRepository;
        this.blockedSlotRepository = blockedSlotRepository;
        this.bookingRepository = bookingRepository;
        this.fileLogger = fileLogger;
    }

    // ---------------- player side ----------------

    /** Search approved venues. Any filter that is null/empty is ignored. */
    public List<Venue> search(SportType sport, String location, String query, BigDecimal maxPrice) {
        String loc = location == null ? "" : location.trim().toLowerCase(Locale.ROOT);
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return venueRepository.findByStatus(VenueStatus.APPROVED).stream()
                .filter(v -> sport == null || v.getSportType() == sport)
                .filter(v -> loc.isEmpty() || v.getLocation().toLowerCase(Locale.ROOT).contains(loc))
                .filter(v -> q.isEmpty() || v.getName().toLowerCase(Locale.ROOT).contains(q))
                .filter(v -> maxPrice == null || v.getPricePerHour().compareTo(maxPrice) <= 0)
                .sorted(Comparator.comparing(Venue::getName))
                .collect(Collectors.toList());
    }

    /** Public view: only approved venues exist for players. */
    public Venue getApprovedVenue(Long id) {
        Venue v = venueRepository.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
        if (v.getStatus() != VenueStatus.APPROVED) {
            throw new VenueNotFoundException(id);
        }
        return v;
    }

    public Venue getVenue(Long id) {
        return venueRepository.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
    }

    public List<Court> getCourts(Long venueId, boolean onlyActive) {
        return onlyActive ? courtRepository.findActiveByVenueId(venueId) : courtRepository.findByVenueId(venueId);
    }

    // ---------------- owner side ----------------

    public Venue createVenue(User owner, VenueRequest r) {
        validateHours(r);
        Venue v = new Venue();
        v.setOwner(owner);
        applyRequest(v, r);
        v.setStatus(VenueStatus.PENDING); // every new venue must be approved by an admin
        Venue saved = venueRepository.save(v);
        fileLogger.info("Venue created: '" + saved.getName() + "' by " + owner.getEmail() + " (waiting for approval)");
        return saved;
    }

    public Venue updateVenue(User owner, Long venueId, VenueRequest r) {
        validateHours(r);
        Venue v = ownedVenue(owner, venueId);
        applyRequest(v, r);
        if (v.getStatus() == VenueStatus.REJECTED) {
            v.setStatus(VenueStatus.PENDING); // re-submitted after rejection
        }
        return venueRepository.save(v);
    }

    public List<Venue> listByOwner(Long ownerId) {
        return venueRepository.findByOwnerId(ownerId);
    }

    public Court addCourt(User owner, Long venueId, CourtRequest r) {
        Venue v = ownedVenue(owner, venueId);
        Court c = new Court();
        c.setVenue(v);
        c.setCourtName(r.courtName().trim());
        c.setSportType(r.sportType());
        return courtRepository.save(c);
    }

    public Court updateCourt(User owner, Long courtId, CourtRequest r) {
        Court c = ownedCourt(owner, courtId);
        c.setCourtName(r.courtName().trim());
        c.setSportType(r.sportType());
        return courtRepository.save(c);
    }

    /** Soft delete: the court disappears from the website but old bookings keep working. */
    public void deactivateCourt(User owner, Long courtId) {
        Court c = ownedCourt(owner, courtId);
        c.setActive(false);
        courtRepository.save(c);
    }

    public BlockedSlot blockSlot(User owner, Long courtId, BlockSlotRequest r) {
        Court court = ownedCourt(owner, courtId);
        Venue v = court.getVenue();
        if (r.date().isBefore(TimeUtil.today())) {
            throw new InvalidRequestException("Cannot block a slot in the past");
        }
        if (!SlotUtil.generateSlots(v.getOpeningTime(), v.getClosingTime()).contains(r.timeSlot())) {
            throw new InvalidRequestException("Invalid time slot for this venue");
        }
        if (bookingRepository.existsActiveBooking(courtId, r.date(), r.timeSlot(),
                EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))) {
            throw new SlotUnavailableException("A booking already exists in this slot. Cancel it first.");
        }
        if (blockedSlotRepository.existsBlocked(courtId, r.date(), r.timeSlot())) {
            throw new SlotUnavailableException("This slot is already blocked");
        }
        BlockedSlot b = new BlockedSlot();
        b.setCourt(court);
        b.setBlockDate(r.date());
        b.setTimeSlot(r.timeSlot());
        b.setReason(r.reason() == null || r.reason().isBlank() ? "Maintenance" : r.reason().trim());
        return blockedSlotRepository.save(b);
    }

    public void unblockSlot(User owner, Long blockId) {
        BlockedSlot b = blockedSlotRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked slot not found"));
        ownedCourt(owner, b.getCourt().getId());
        blockedSlotRepository.delete(b);
    }

    public List<BlockedSlot> listBlocks(User owner, Long courtId) {
        ownedCourt(owner, courtId);
        return blockedSlotRepository.findUpcomingForCourt(courtId, TimeUtil.today());
    }

    // ---------------- admin side ----------------

    public List<Venue> listAll(VenueStatus status) {
        List<Venue> all = status == null ? venueRepository.findAll() : venueRepository.findByStatus(status);
        return all.stream().sorted(Comparator.comparing(Venue::getId).reversed()).collect(Collectors.toList());
    }

    public Venue review(Long venueId, boolean approve, String comment) {
        Venue v = getVenue(venueId);
        v.setStatus(approve ? VenueStatus.APPROVED : VenueStatus.REJECTED);
        v.setAdminComment(comment == null || comment.isBlank() ? null : comment.trim());
        fileLogger.info("Admin " + (approve ? "APPROVED" : "REJECTED") + " venue '" + v.getName() + "'");
        return venueRepository.save(v);
    }

    // ---------------- helpers ----------------

    private void validateHours(VenueRequest r) {
        if (!r.closingTime().isAfter(r.openingTime())) {
            throw new InvalidRequestException("Closing time must be after opening time");
        }
        if (SlotUtil.generateSlots(r.openingTime(), r.closingTime()).isEmpty()) {
            throw new InvalidRequestException("Opening hours must allow at least one 1-hour slot");
        }
    }

    private void applyRequest(Venue v, VenueRequest r) {
        v.setName(r.name().trim());
        v.setLocation(r.location().trim());
        v.setDescription(r.description());
        v.setSportType(r.sportType());
        v.setPricePerHour(r.pricePerHour());
        v.setOpeningTime(r.openingTime());
        v.setClosingTime(r.closingTime());
        v.setAmenities(r.amenities());
        String url = r.imageUrl() == null ? "" : r.imageUrl().trim();
        // only accept normal web links (blocks javascript: and similar tricks)
        v.setImageUrl(url.startsWith("http://") || url.startsWith("https://") ? url : null);
    }

    private Venue ownedVenue(User owner, Long venueId) {
        Venue v = venueRepository.findById(venueId).orElseThrow(() -> new VenueNotFoundException(venueId));
        if (!v.getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException("You do not own this venue");
        }
        return v;
    }

    private Court ownedCourt(User owner, Long courtId) {
        Court c = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id " + courtId));
        if (!c.getVenue().getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException("You do not own this court");
        }
        return c;
    }
}
