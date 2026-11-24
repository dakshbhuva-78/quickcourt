package com.quickcourt.exception;

/** Thrown when the chosen court/date/time slot is already booked or blocked. -> HTTP 409 */
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}
