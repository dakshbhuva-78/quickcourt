package com.quickcourt.controller;

import com.quickcourt.dto.AdminDecisionRequest;
import com.quickcourt.dto.StatsResponse;
import com.quickcourt.model.*;
import com.quickcourt.service.BookingService;
import com.quickcourt.service.StatsService;
import com.quickcourt.service.UserService;
import com.quickcourt.service.VenueService;
import com.quickcourt.util.FileLogger;
import com.quickcourt.util.ReportGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/** Admin area. AuthInterceptor makes sure only ADMIN users can reach /api/admin/**. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final VenueService venueService;
    private final BookingService bookingService;
    private final StatsService statsService;
    private final FileLogger fileLogger;
    private final String reportDir;

    public AdminController(UserService userService, VenueService venueService, BookingService bookingService,
                           StatsService statsService, FileLogger fileLogger,
                           @Value("${quickcourt.report.dir:reports}") String reportDir) {
        this.userService = userService;
        this.venueService = venueService;
        this.bookingService = bookingService;
        this.statsService = statsService;
        this.fileLogger = fileLogger;
        this.reportDir = reportDir;
    }

    @GetMapping("/users")
    public List<User> users(@RequestParam(required = false) Role role, @RequestParam(required = false) String q) {
        return userService.searchUsers(role, q);
    }

    @PutMapping("/users/{id}/ban")
    public User ban(@PathVariable Long id) {
        return userService.setActive(id, false);
    }

    @PutMapping("/users/{id}/unban")
    public User unban(@PathVariable Long id) {
        return userService.setActive(id, true);
    }

    @GetMapping("/users/{id}/bookings")
    public List<Booking> userBookings(@PathVariable Long id) {
        return bookingService.getBookingsOfUser(id);
    }

    @GetMapping("/venues")
    public List<Venue> venues(@RequestParam(required = false) VenueStatus status) {
        return venueService.listAll(status);
    }

    @GetMapping("/venues/{id}/courts")
    public List<Court> venueCourts(@PathVariable Long id) {
        return venueService.getCourts(id, false);
    }

    @PutMapping("/venues/{id}/approve")
    public Venue approve(@PathVariable Long id, @RequestBody(required = false) AdminDecisionRequest body) {
        return venueService.review(id, true, body == null ? null : body.comment());
    }

    @PutMapping("/venues/{id}/reject")
    public Venue reject(@PathVariable Long id, @RequestBody(required = false) AdminDecisionRequest body) {
        return venueService.review(id, false, body == null ? null : body.comment());
    }

    @GetMapping("/bookings")
    public List<Booking> bookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return statsService.forAdmin();
    }

    /** Generates a text report, saves a copy in reports/ and returns it as a download. */
    @GetMapping("/report")
    public ResponseEntity<String> report() throws IOException {
        String text = ReportGenerator.buildReport(statsService.forAdmin());
        ReportGenerator.saveToFile(text, reportDir);
        fileLogger.info("Platform report generated");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quickcourt_report.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(text);
    }

    /** Reads the last lines of the application log file. */
    @GetMapping("/logs")
    public List<String> logs(@RequestParam(defaultValue = "30") int lines) {
        return fileLogger.tail(Math.min(lines, 200));
    }
}
