package com.quickcourt.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for the file logger (write + read back). */
class FileLoggerTest {

    @Test
    void writesAndReadsLines(@TempDir Path dir) {
        FileLogger logger = new FileLogger(dir.toString());
        logger.info("first");
        logger.warn("second");
        List<String> lines = logger.tail(10);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("[INFO] first"));
        assertTrue(lines.get(1).contains("[WARN] second"));
        assertTrue(Files.exists(logger.getLogFile()));
    }

    @Test
    void tailReturnsOnlyLastLines(@TempDir Path dir) {
        FileLogger logger = new FileLogger(dir.toString());
        for (int i = 1; i <= 5; i++) {
            logger.info("line " + i);
        }
        List<String> lines = logger.tail(2);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("line 5"));
    }

    @Test
    void tailOfMissingFileIsEmpty(@TempDir Path dir) {
        assertTrue(new FileLogger(dir.resolve("nothing").toString()).tail(5).isEmpty());
    }
}
