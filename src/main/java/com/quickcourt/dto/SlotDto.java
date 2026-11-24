package com.quickcourt.dto;

/** One hourly slot with its availability, shown on the booking page. */
public record SlotDto(String timeSlot, boolean available, String reason) {
}
