package com.quickcourt.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * MULTITHREADING: a Runnable that runs on a BACKGROUND thread.
 * It pretends to send an email/SMS (waits 1.5 s) and then writes the message to notifications.log.
 * Because it runs in the background, the player gets the booking response immediately
 * and does not have to wait for the "email" to be sent.
 */
public class NotificationTask implements Runnable {

    private final String message;
    private final Path file;

    public NotificationTask(String message, Path file) {
        this.message = message;
        this.file = file;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1500); // simulate slow email/SMS gateway
            Files.createDirectories(file.getParent());
            String line = TimeUtil.now() + " [" + Thread.currentThread().getName() + "] " + message + System.lineSeparator();
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupt flag and stop
        } catch (IOException e) {
            System.err.println("Could not write notification: " + e.getMessage());
        }
    }
}
