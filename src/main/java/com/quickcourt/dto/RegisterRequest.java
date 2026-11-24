package com.quickcourt.dto;

import com.quickcourt.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Data sent by the registration form. Validation rules are checked automatically. */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 60, message = "Name must be 2-60 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Password needs 8+ characters with upper-case, lower-case and a digit")
        String password,

        @NotNull(message = "Role is required")
        Role role) {
}
