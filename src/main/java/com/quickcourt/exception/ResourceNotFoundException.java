package com.quickcourt.exception;

/** Generic "not found" for courts, bookings, users ... -> HTTP 404 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
