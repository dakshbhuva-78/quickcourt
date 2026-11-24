package com.quickcourt.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** One place for "what time is it now?" - always India time, even if the cloud server runs in UTC. */
public final class TimeUtil {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private TimeUtil() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
