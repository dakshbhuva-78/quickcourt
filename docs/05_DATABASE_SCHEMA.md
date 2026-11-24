# 5. Database Schema

Database: **MySQL 8**, name `quickcourt`. Tables are created automatically by Hibernate. Reference DDL: `database/schema.sql`.

## 5.1 ER diagram
```mermaid
erDiagram
    USERS ||--o{ VENUES : "owns (owner_id)"
    USERS ||--o{ BOOKINGS : "makes (user_id)"
    VENUES ||--o{ COURTS : "has"
    VENUES ||--o{ BOOKINGS : "receives"
    COURTS ||--o{ BOOKINGS : "booked as"
    COURTS ||--o{ BLOCKED_SLOTS : "blocked in"
    BOOKINGS ||--o{ PAYMENTS : "paid by"

    USERS {
        bigint id PK
        varchar user_type "PLAYER | VENUE_OWNER | ADMIN"
        varchar name
        varchar email UK
        varchar password "BCrypt hash"
        varchar role
        bit active
        datetime created_at
    }
    VENUES {
        bigint id PK
        varchar name
        varchar location
        varchar description
        varchar sport_type
        decimal price_per_hour
        time opening_time
        time closing_time
        varchar amenities
        varchar image_url
        varchar status "PENDING | APPROVED | REJECTED"
        varchar admin_comment
        bigint owner_id FK
    }
    COURTS {
        bigint id PK
        varchar court_name
        varchar sport_type
        bit active
        bigint venue_id FK
    }
    BOOKINGS {
        bigint id PK
        bigint user_id FK
        bigint venue_id FK
        bigint court_id FK
        date booking_date
        varchar time_slot "18:00-19:00"
        decimal amount
        varchar booking_status
        datetime created_at
    }
    PAYMENTS {
        bigint id PK
        bigint booking_id FK
        decimal amount
        varchar payment_method "UPI | CARD | CASH"
        varchar payment_status "PENDING | SUCCESS | FAILED"
        varchar transaction_reference
        datetime created_at
    }
    BLOCKED_SLOTS {
        bigint id PK
        bigint court_id FK
        date block_date
        varchar time_slot
        varchar reason
    }
```

## 5.2 Entities required by the brief
| Required entity | Implemented as | Notes |
|---|---|---|
| User (id, name, email, password, role, createdAt) | `users` / `User` + 3 subclasses | `active` added for ban/unban |
| Venue (…, owner, status) | `venues` / `Venue` | opening/closing hours, amenities, image, admin comment added |
| Court (id, courtName, sportType, venue) | `courts` / `Court` | `active` added (soft delete) |
| Booking (…) | `bookings` / `Booking` | |
| Payment (…) | `payments` / `Payment` | |
| *(extra)* BlockedSlot | `blocked_slots` | needed for the owner's "block time slots for maintenance" |

## 5.3 Normalisation (short note)
- **1NF:** every column holds one value (exception: `venues.amenities` is a comma-separated text for MVP simplicity; normalise to an `amenities` table later).
- **2NF/3NF:** venue data lives only in `venues`, court data only in `courts`; bookings reference them by id, so changing a venue name does not require updating bookings.

## 5.4 Sample data (created by `DataSeeder`)
- 6 users: 1 admin, 2 owners, 3 players (password rules shown in README).
- 7 venues in Ahmedabad: 6 APPROVED (badminton, football, tennis, cricket, table tennis, basketball) and 1 PENDING ("Ace Badminton Studio").
- 14 courts, 7 bookings (past, future, one cancelled) with matching payments.

## 5.5 Useful queries
See `database/useful_queries.sql` (users, pending venues, booking history, revenue per venue, a query that proves there are no double bookings).
