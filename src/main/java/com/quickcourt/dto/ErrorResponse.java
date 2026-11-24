package com.quickcourt.dto;

import java.time.LocalDateTime;

/** Standard error body returned by GlobalExceptionHandler. */
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
}
