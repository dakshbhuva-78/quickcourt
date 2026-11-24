package com.quickcourt.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Numbers used by the owner and admin dashboards (KPIs + chart data). */
public record StatsResponse(
        long totalBookings,
        long confirmedBookings,
        long cancelledBookings,
        long activeCourts,
        long totalVenues,
        long pendingVenues,
        long totalUsers,
        long totalOwners,
        BigDecimal earnings,
        Map<String, Long> bookingsByDay,
        Map<String, Long> bookingsBySport,
        Map<String, Long> bookingsByHour,
        Map<String, BigDecimal> earningsBySport,
        Map<String, Long> registrationsByDay,
        Map<String, Long> venuesByStatus) {
}
