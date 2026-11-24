package com.quickcourt.exception;

/** Thrown for invalid booking input (past date, wrong slot, venue not approved ...). -> HTTP 400 */
public class InvalidBookingException extends RuntimeException {
    public InvalidBookingException(String message) {
        super(message);
    }
}
