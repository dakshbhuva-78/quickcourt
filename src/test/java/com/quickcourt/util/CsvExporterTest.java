package com.quickcourt.util;

import com.quickcourt.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for CSV export (file handling). */
class CsvExporterTest {

    private Booking sampleBooking(String playerName) {
        Player p = new Player(playerName, "a@x.com", "hash");
        p.setId(1L);
        Venue v = new Venue();
        v.setName("Green Arena");
        v.setSportType(SportType.BADMINTON);
        Court c = new Court();
        c.setCourtName("Court 1");
        Booking b = new Booking();
        b.setId(5L);
        b.setUser(p);
        b.setVenue(v);
        b.setCourt(c);
        b.setBookingDate(LocalDate.of(2030, 1, 15));
        b.setTimeSlot("18:00-19:00");
        b.setAmount(new BigDecimal("400"));
        b.setBookingStatus(BookingStatus.CONFIRMED);
        return b;
    }

    @Test
    void csvHasHeaderAndRow() {
        String csv = CsvExporter.toCsv(List.of(sampleBooking("Aarav")));
        String[] lines = csv.split("\n");
        assertEquals("BookingId,Player,Venue,Court,Sport,Date,TimeSlot,Amount,Status,CreatedAt", lines[0]);
        assertTrue(lines[1].startsWith("5,Aarav,Green Arena,Court 1,BADMINTON,2030-01-15,18:00-19:00,400,CONFIRMED"));
    }

    @Test
    void emptyListGivesOnlyHeader() {
        assertEquals(1, CsvExporter.toCsv(List.of()).split("\n").length);
    }

    @Test
    void commasAreQuoted() {
        assertEquals("\"Smith, John\"", CsvExporter.escape("Smith, John"));
    }

    @Test
    void quotesAreDoubled() {
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""));
    }

    @Test
    void formulaInjectionIsNeutralised() {
        assertTrue(CsvExporter.escape("=SUM(A1:A9)").startsWith("'="));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", CsvExporter.escape(null));
    }

    @Test
    void savesFileToDisk(@TempDir Path dir) throws Exception {
        Path file = CsvExporter.saveToFile("a,b\n", dir.toString(), "test");
        assertTrue(Files.exists(file));
        assertEquals("a,b\n", Files.readString(file));
    }
}
