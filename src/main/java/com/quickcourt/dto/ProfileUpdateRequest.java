package com.quickcourt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 60, message = "Name must be 2-60 characters")
        String name) {
}
