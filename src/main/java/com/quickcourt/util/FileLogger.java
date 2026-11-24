package com.quickcourt.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

/**
 * FILE HANDLING: writes application events to logs/quickcourt.log and can read the last lines back.
 * "synchronized" makes sure two threads never write to the file at the same moment.
 */
@Component
public class FileLogger {

    private final Path logFile;

    public FileLogger(@Value("${quickcourt.log.dir:logs}") String logDir) {
        this.logFile = Paths.get(logDir, "quickcourt.log");
    }

    public synchronized void log(String level, String message) {
        try {
            Files.createDirectories(logFile.getParent());
            String line = TimeUtil.now() + " [" + level + "] " + message + System.lineSeparator();
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Could not write log file: " + e.getMessage());
        }
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void warn(String message) {
        log("WARN", message);
    }

    /** Reads the last N lines of the log file (newest last). */
    public synchronized List<String> tail(int maxLines) {
        try {
            if (!Files.exists(logFile)) {
                return Collections.emptyList();
            }
            List<String> all = Files.readAllLines(logFile);
            int from = Math.max(0, all.size() - Math.max(1, maxLines));
            return all.subList(from, all.size());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public Path getLogFile() {
        return logFile;
    }
}
