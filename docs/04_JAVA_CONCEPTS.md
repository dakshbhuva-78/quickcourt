# 4. Java Concepts Used in QuickCourt (with viva answers)

| Concept | Where to find it | One-line explanation |
|---|---|---|
| Classes & objects | `model/*.java` | `Venue`, `Court`, `Booking`, `Payment` are classes; each row in the database is an object |
| Encapsulation | `model/User.java` etc. | Fields are `private`; accessed via getters/setters. `password` has `@JsonIgnore` so it is never sent out |
| Abstraction | `User` (abstract class), `PaymentService` (interface) | Hide details, expose only what callers need |
| Inheritance | `User` → `Player`, `VenueOwner`, `Admin` | Subclasses reuse id/name/email/password and add their own behaviour |
| Polymorphism | `getDashboardPath()` | Same method, each subclass returns a different page |
| Interface | `service/PaymentService.java` → `MockPaymentService.java` | Controller depends on the interface; real gateway can replace the mock later |
| Custom exceptions | `exception/*.java` | `SlotUnavailableException`, `InvalidBookingException`, `VenueNotFoundException`, … |
| Exception handling | `GlobalExceptionHandler.java` | Converts exceptions into clean JSON + correct HTTP status |
| Multithreading | `NotificationService.java`, `util/NotificationTask.java` | Booking confirmation "email" runs on a background thread |
| File handling | `FileLogger`, `CsvExporter`, `ReportGenerator` | Writes logs, exports CSV, generates reports, reads log tail |
| Collections & streams | `VenueService.search`, `StatsService` | `List`, `Map`, `Set`, `stream().filter().collect()` |
| Records (Java 16+) | `dto/*.java` | Short immutable data carriers for requests/responses |
| Enums | `Role`, `SportType`, `BookingStatus` … | Fixed sets of allowed values |
| Dependency injection | Constructors in services/controllers | Spring creates objects and passes them in |
| Synchronization | `BookingService.createBooking` (`synchronized`), `FileLogger.log` | Prevents two threads from booking the same slot / writing the log at once |

## 4.1 Inheritance in detail
```
            User (abstract)
   ┌──────────┼───────────┐
 Player   VenueOwner     Admin
```
`User` is stored in one table `users`; the column `user_type` says which subclass (JPA `SINGLE_TABLE`). `UserService.register()` creates a `Player` or a `VenueOwner` depending on the role chosen.

## 4.2 The thread — purpose and flow
**Problem:** sending an email/SMS is slow (1–2 s). If the booking request waited for it, the player would stare at a spinner.
**Solution:** `MockPaymentService` calls `notificationService.sendBookingConfirmation(booking)`. That method creates a `NotificationTask` (a `Runnable`) and submits it to an `ExecutorService` with 2 worker threads. The HTTP response returns immediately; later the worker thread waits 1.5 s (simulating the email gateway) and appends a line to `logs/notifications.log` that includes the thread name (e.g. `pool-2-thread-1`) — visible proof it ran in the background.
`@PreDestroy shutdown()` stops the thread pool when the application stops.

## 4.3 Double-booking prevention (a good viva question)
1. `BookingService.createBooking` is `synchronized`, so only one thread runs it at a time in one server.
2. It asks the database: *is there a PENDING/CONFIRMED/COMPLETED booking for this court + date + slot?* If yes → `SlotUnavailableException` (HTTP 409).
3. Cancelled bookings do not count, so a cancelled slot becomes free again.
4. Limitation / improvement: with several server instances you would add a database-level unique constraint or a locking strategy (e.g. a unique "active slot key" column or pessimistic locking).

## 4.4 Likely viva questions
1. **Why abstract `User`?** We never need a "plain user"; every user has a role. Abstract forces us to create `Player`/`VenueOwner`/`Admin`.
2. **Interface vs abstract class?** Interface = contract without state (`PaymentService`); abstract class = shared state + partial behaviour (`User`).
3. **Why a custom exception instead of `RuntimeException`?** Clear meaning, mapped to the right HTTP code in one place.
4. **Checked vs unchecked exceptions?** Ours extend `RuntimeException` (unchecked) so service code stays clean; `IOException` (checked) in file code is caught and logged.
5. **Why BCrypt?** Slow, salted one-way hash. Even if the database leaks, passwords cannot be read directly.
6. **What does `@Service` / `@RestController` / `@Entity` do?** Marks a class as a Spring bean / REST controller / database table.
7. **What is DTO?** A small object for transferring request/response data so entities (and password hashes) are not exposed.
8. **How does role-based access work?** `AuthInterceptor` checks the token; `/api/admin/**` needs ADMIN, `/api/owner/**` needs VENUE_OWNER; services also check ownership (e.g. you can edit only your venue).
9. **What if two users book at the same moment?** `synchronized` + availability check → the second gets 409 (see 4.3).
10. **Why `ExecutorService` instead of `new Thread()`?** Reuses threads, limits how many run, and can be shut down cleanly.
