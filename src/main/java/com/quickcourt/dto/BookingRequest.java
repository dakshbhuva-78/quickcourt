package com.quickcourt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Player's choice: venue + court + date + slot ("18:00-19:00"). */
public record BookingRequest(
        @NotNull(message = "Venue is required") Long venueId,
        @NotNull(message = "Court is required") Long courtId,
        @NotNull(message = "Date is required") LocalDate bookingDate,
        @NotBlank(message = "Time slot is required") String timeSlot) {
}
