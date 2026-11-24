package com.quickcourt.service;

import com.quickcourt.model.Booking;
import com.quickcourt.util.NotificationTask;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MULTITHREADING: keeps a small pool of 2 background worker threads.
 * Sending a notification takes time, so it is handed to a worker thread and the
 * HTTP request can finish at once. See NotificationTask for the work done in the thread.
 */
@Service
public class NotificationService {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Path notificationFile;

    public NotificationService(@Value("${quickcourt.log.dir:logs}") String logDir) {
        this.notificationFile = Paths.get(logDir, "notifications.log");
    }

    public void sendBookingConfirmation(Booking b) {
        String message = "CONFIRMATION to " + b.getUser().getName() + ": booking #" + b.getId() + " at "
                + b.getVenue().getName() + " (" + b.getCourt().getCourtName() + ") on "
                + b.getBookingDate() + " " + b.getTimeSlot();
        executor.submit(new NotificationTask(message, notificationFile));
    }

    public void sendCancellation(Booking b) {
        String message = "CANCELLATION to " + b.getUser().getName() + ": booking #" + b.getId() + " at "
                + b.getVenue().getName() + " on " + b.getBookingDate() + " " + b.getTimeSlot();
        executor.submit(new NotificationTask(message, notificationFile));
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
