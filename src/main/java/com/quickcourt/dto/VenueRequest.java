package com.quickcourt.dto;

import com.quickcourt.model.SportType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Used by owners to add or edit a venue. */
public record VenueRequest(
        @NotBlank(message = "Venue name is required") @Size(max = 100) String name,
        @NotBlank(message = "Location is required") @Size(max = 150) String location,
        @Size(max = 2000) String description,
        @NotNull(message = "Sport type is required") SportType sportType,
        @NotNull(message = "Price is required") @DecimalMin(value = "1.0", message = "Price must be at least 1") BigDecimal pricePerHour,
        @NotNull(message = "Opening time is required") LocalTime openingTime,
        @NotNull(message = "Closing time is required") LocalTime closingTime,
        @Size(max = 500) String amenities,
        @Size(max = 500) String imageUrl) {
}
