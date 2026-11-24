package com.quickcourt.util;

import com.quickcourt.dto.StatsResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** FILE HANDLING: builds a plain-text platform report and saves it in the reports/ folder. */
public final class ReportGenerator {

    private ReportGenerator() {
    }

    public static String buildReport(StatsResponse s) {
        StringBuilder sb = new StringBuilder();
        sb.append("QUICKCOURT PLATFORM REPORT\n");
        sb.append("Generated: ").append(TimeUtil.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        sb.append("==========================================\n");
        sb.append("Total users           : ").append(s.totalUsers()).append("\n");
        sb.append("Venue owners          : ").append(s.totalOwners()).append("\n");
        sb.append("Venues (all)          : ").append(s.totalVenues()).append("\n");
        sb.append("Venues pending review : ").append(s.pendingVenues()).append("\n");
        sb.append("Active courts         : ").append(s.activeCourts()).append("\n");
        sb.append("Total bookings        : ").append(s.totalBookings()).append("\n");
        sb.append("Confirmed/completed   : ").append(s.confirmedBookings()).append("\n");
        sb.append("Cancelled             : ").append(s.cancelledBookings()).append("\n");
        sb.append("Simulated earnings    : INR ").append(s.earnings()).append("\n\n");
        sb.append("Bookings by sport\n");
        for (Map.Entry<String, Long> e : s.bookingsBySport().entrySet()) {
            sb.append("  - ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        sb.append("\n(All earnings are simulated. No real payments are processed.)\n");
        return sb.toString();
    }

    public static Path saveToFile(String report, String dir) throws IOException {
        Path folder = Paths.get(dir);
        Files.createDirectories(folder);
        String stamp = TimeUtil.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return Files.writeString(folder.resolve("report_" + stamp + ".txt"), report);
    }
}
