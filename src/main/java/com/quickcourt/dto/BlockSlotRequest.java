package com.quickcourt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BlockSlotRequest(
        @NotNull(message = "Date is required") LocalDate date,
        @NotBlank(message = "Time slot is required") String timeSlot,
        String reason) {
}
