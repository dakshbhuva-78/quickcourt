package com.quickcourt.exception;

import com.quickcourt.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * One place that converts Java exceptions into clean JSON error responses,
 * so the frontend always receives {status, error, message, timestamp}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleSlot(SlotUnavailableException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUser(UserAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "Data conflict: this record already exists or is in use");
    }

    @ExceptionHandler({InvalidBookingException.class, InvalidRequestException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({VenueNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationFailedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request body. Check dates (yyyy-MM-dd), times (HH:mm) and option values.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
    }
}
