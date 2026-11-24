package com.quickcourt.controller;

import com.quickcourt.dto.BookingRequest;
import com.quickcourt.model.Booking;
import com.quickcourt.model.BookingStatus;
import com.quickcourt.model.User;
import com.quickcourt.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Booking endpoints for logged-in users. */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@RequestAttribute("currentUser") User user, @Valid @RequestBody BookingRequest request) {
        return bookingService.createBooking(user, request);
    }

    @GetMapping("/my")
    public List<Booking> myBookings(@RequestAttribute("currentUser") User user,
                                    @RequestParam(required = false) BookingStatus status) {
        return bookingService.getMyBookings(user.getId(), status);
    }

    @GetMapping("/{id}")
    public Booking one(@RequestAttribute("currentUser") User user, @PathVariable Long id) {
        return bookingService.getBookingFor(id, user);
    }

    @PutMapping("/{id}/cancel")
    public Booking cancel(@RequestAttribute("currentUser") User user, @PathVariable Long id) {
        return bookingService.cancelBooking(id, user);
    }

    /** CSV download. Player = own bookings, owner = bookings of own venues, admin = all. */
    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestAttribute("currentUser") User user) {
        String csv = bookingService.exportCsv(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quickcourt_bookings.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
