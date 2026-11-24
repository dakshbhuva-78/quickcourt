package com.quickcourt.model;

/**
 * PENDING   = slot reserved, payment not finished yet
 * CONFIRMED = payment done (or cash-at-venue chosen)
 * CANCELLED = cancelled by player/admin, slot is free again
 * COMPLETED = confirmed booking whose time has passed
 */
public enum BookingStatus {
    PENDING, CONFIRMED, CANCELLED, COMPLETED
}
