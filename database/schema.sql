-- QuickCourt reference schema (MySQL 8).
-- You do NOT need to run this file: Spring Boot / Hibernate creates these tables automatically
-- (spring.jpa.hibernate.ddl-auto=update). It is here so you can explain the database in the viva.
-- Column types are shown in plain form; Hibernate may create MySQL ENUM columns for enums.

CREATE DATABASE IF NOT EXISTS quickcourt CHARACTER SET utf8mb4;
USE quickcourt;

-- One table for Player, VenueOwner and Admin (inheritance: SINGLE_TABLE). user_type is the discriminator.
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_type   VARCHAR(31)  NOT NULL,              -- PLAYER | VENUE_OWNER | ADMIN
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,              -- BCrypt hash, never plain text
    role        VARCHAR(20)  NOT NULL,
    active      BIT          NOT NULL DEFAULT 1,    -- 0 = banned
    created_at  DATETIME
);

CREATE TABLE venues (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    location       VARCHAR(255) NOT NULL,
    description    VARCHAR(2000),
    sport_type     VARCHAR(20)  NOT NULL,
    price_per_hour DECIMAL(10,2) NOT NULL,
    opening_time   TIME,
    closing_time   TIME,
    amenities      VARCHAR(500),
    image_url      VARCHAR(500),
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',   -- PENDING | APPROVED | REJECTED
    admin_comment  VARCHAR(500),
    owner_id       BIGINT NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE courts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    court_name VARCHAR(255) NOT NULL,
    sport_type VARCHAR(20)  NOT NULL,
    active     BIT NOT NULL DEFAULT 1,
    venue_id   BIGINT NOT NULL,
    FOREIGN KEY (venue_id) REFERENCES venues(id)
);

CREATE TABLE bookings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    venue_id       BIGINT NOT NULL,
    court_id       BIGINT NOT NULL,
    booking_date   DATE NOT NULL,
    time_slot      VARCHAR(11) NOT NULL,              -- e.g. 18:00-19:00
    amount         DECIMAL(10,2) NOT NULL,
    booking_status VARCHAR(20) NOT NULL,              -- PENDING | CONFIRMED | CANCELLED | COMPLETED
    created_at     DATETIME,
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (venue_id) REFERENCES venues(id),
    FOREIGN KEY (court_id) REFERENCES courts(id),
    INDEX idx_slot_lookup (court_id, booking_date, time_slot)
);

CREATE TABLE payments (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id            BIGINT NOT NULL,
    amount                DECIMAL(10,2) NOT NULL,
    payment_method        VARCHAR(10) NOT NULL,       -- UPI | CARD | CASH
    payment_status        VARCHAR(10) NOT NULL,       -- PENDING | SUCCESS | FAILED
    transaction_reference VARCHAR(255),
    created_at            DATETIME,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE blocked_slots (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    court_id   BIGINT NOT NULL,
    block_date DATE NOT NULL,
    time_slot  VARCHAR(11) NOT NULL,
    reason     VARCHAR(255),
    FOREIGN KEY (court_id) REFERENCES courts(id)
);
