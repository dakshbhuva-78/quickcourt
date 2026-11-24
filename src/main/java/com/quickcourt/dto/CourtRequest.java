package com.quickcourt.dto;

import com.quickcourt.model.SportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourtRequest(
        @NotBlank(message = "Court name is required") @Size(max = 60) String courtName,
        @NotNull(message = "Sport type is required") SportType sportType) {
}
