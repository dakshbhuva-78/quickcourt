package com.quickcourt.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for hourly time slots written as "HH:mm-HH:mm" (example: "18:00-19:00").
 */
public final class SlotUtil {

    private SlotUtil() {
    }

    /** Builds hourly slots between opening and closing time. 06:00-09:00 gives 3 slots. */
    public static List<String> generateSlots(LocalTime open, LocalTime close) {
        List<String> slots = new ArrayList<>();
        if (open == null || close == null) {
            return slots;
        }
        LocalTime start = open;
        while (true) {
            LocalTime end = start.plusHours(1);
            // stop when we pass midnight or go beyond closing time
            if (!end.isAfter(start) || end.isAfter(close)) {
                break;
            }
            slots.add(start + "-" + end);
            start = end;
        }
        return slots;
    }

    public static boolean isValidFormat(String slot) {
        return slot != null && slot.matches("^\\d{2}:\\d{2}-\\d{2}:\\d{2}$");
    }

    public static LocalTime startOf(String slot) {
        return LocalTime.parse(slot.substring(0, 5));
    }

    public static LocalTime endOf(String slot) {
        return LocalTime.parse(slot.substring(6, 11));
    }

    public static LocalDateTime startDateTime(LocalDate date, String slot) {
        return LocalDateTime.of(date, startOf(slot));
    }

    public static LocalDateTime endDateTime(LocalDate date, String slot) {
        return LocalDateTime.of(date, endOf(slot));
    }
}
