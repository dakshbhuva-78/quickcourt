package com.quickcourt.exception;

/** Thrown when a venue id does not exist (or is not visible). -> HTTP 404 */
public class VenueNotFoundException extends RuntimeException {
    public VenueNotFoundException(Long id) {
        super("Venue not found with id " + id);
    }
}
