-- Handy queries to show during the viva (run in MySQL Workbench or the mysql console).
USE quickcourt;

-- 1. All users and their roles
SELECT id, name, email, role, active FROM users;

-- 2. Approved venues with price
SELECT id, name, location, sport_type, price_per_hour FROM venues WHERE status = 'APPROVED';

-- 3. Venues waiting for admin approval
SELECT id, name, location FROM venues WHERE status = 'PENDING';

-- 4. Booking history with player and venue names
SELECT b.id, u.name AS player, v.name AS venue, c.court_name, b.booking_date, b.time_slot, b.amount, b.booking_status
FROM bookings b
JOIN users u  ON u.id = b.user_id
JOIN venues v ON v.id = b.venue_id
JOIN courts c ON c.id = b.court_id
ORDER BY b.booking_date DESC;

-- 5. Revenue per venue (simulated)
SELECT v.name, COUNT(*) AS bookings, SUM(b.amount) AS earnings
FROM bookings b JOIN venues v ON v.id = b.venue_id
WHERE b.booking_status IN ('CONFIRMED', 'COMPLETED')
GROUP BY v.name ORDER BY earnings DESC;

-- 6. Prove there are no double bookings (should return 0 rows)
SELECT court_id, booking_date, time_slot, COUNT(*) AS n
FROM bookings
WHERE booking_status IN ('PENDING', 'CONFIRMED', 'COMPLETED')
GROUP BY court_id, booking_date, time_slot
HAVING n > 1;

-- 7. Payments by status
SELECT payment_status, COUNT(*) FROM payments GROUP BY payment_status;
