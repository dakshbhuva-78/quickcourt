package com.quickcourt.util;

import com.quickcourt.model.Booking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** FILE HANDLING: converts bookings to CSV text and saves a copy on disk. */
public final class CsvExporter {

    private static final String HEADER = "BookingId,Player,Venue,Court,Sport,Date,TimeSlot,Amount,Status,CreatedAt";

    private CsvExporter() {
    }

    public static String toCsv(List<Booking> bookings) {
        StringBuilder sb = new StringBuilder(HEADER).append("\n");
        for (Booking b : bookings) {
            sb.append(b.getId()).append(',')
              .append(escape(b.getUser().getName())).append(',')
              .append(escape(b.getVenue().getName())).append(',')
              .append(escape(b.getCourt().getCourtName())).append(',')
              .append(b.getVenue().getSportType()).append(',')
              .append(b.getBookingDate()).append(',')
              .append(b.getTimeSlot()).append(',')
              .append(b.getAmount()).append(',')
              .append(b.getBookingStatus()).append(',')
              .append(b.getCreatedAt() == null ? "" : b.getCreatedAt().toString())
              .append("\n");
        }
        return sb.toString();
    }

    /** Wraps values containing commas/quotes in quotes and neutralises Excel formula injection (=, +, -, @). */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (v.matches("^[=+\\-@].*")) {
            v = "'" + v;
        }
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n");
        v = v.replace("\"", "\"\"");
        return needsQuotes ? "\"" + v + "\"" : v;
    }

    /** Saves the CSV text as exports/<prefix>_<timestamp>.csv and returns the path. */
    public static Path saveToFile(String csv, String dir, String prefix) throws IOException {
        Path folder = Paths.get(dir);
        Files.createDirectories(folder);
        String stamp = TimeUtil.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = folder.resolve(prefix + "_" + stamp + ".csv");
        return Files.writeString(file, csv);
    }
}
