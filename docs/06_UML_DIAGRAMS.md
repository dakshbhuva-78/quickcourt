# 6. UML and Architecture Diagrams

(Mermaid diagrams: GitHub and VS Code (extension *Markdown Preview Mermaid Support*) render them. For the report, take screenshots or paste the code into https://mermaid.live and export PNG/SVG.)

## 6.1 Class diagram
```mermaid
classDiagram
    class User {
        <<abstract>>
        -Long id
        -String name
        -String email
        -String password
        -Role role
        -boolean active
        -LocalDateTime createdAt
        +getDashboardPath() String*
    }
    class Player { +getDashboardPath() String }
    class VenueOwner { +getDashboardPath() String }
    class Admin { +getDashboardPath() String }
    User <|-- Player
    User <|-- VenueOwner
    User <|-- Admin

    class Venue {
        -Long id
        -String name
        -String location
        -SportType sportType
        -BigDecimal pricePerHour
        -LocalTime openingTime
        -LocalTime closingTime
        -VenueStatus status
    }
    class Court {
        -Long id
        -String courtName
        -SportType sportType
        -boolean active
    }
    class Booking {
        -Long id
        -LocalDate bookingDate
        -String timeSlot
        -BigDecimal amount
        -BookingStatus bookingStatus
    }
    class Payment {
        -Long id
        -BigDecimal amount
        -PaymentMethod paymentMethod
        -PaymentStatus paymentStatus
        -String transactionReference
    }
    class BlockedSlot {
        -LocalDate blockDate
        -String timeSlot
    }
    VenueOwner "1" --> "*" Venue : owns
    Venue "1" *-- "*" Court
    Player "1" --> "*" Booking : makes
    Court "1" --> "*" Booking
    Venue "1" --> "*" Booking
    Booking "1" --> "*" Payment
    Court "1" --> "*" BlockedSlot

    class PaymentService {
        <<interface>>
        +processPayment(Booking, PaymentMethod, String, String) Payment
        +getPaymentForBooking(Long) Payment
    }
    class MockPaymentService
    PaymentService <|.. MockPaymentService

    class BookingService {
        +createBooking(User, BookingRequest) Booking
        +cancelBooking(Long, User) Booking
        +getSlots(Long, Long, LocalDate) List~SlotDto~
        +exportCsv(User) String
    }
    class NotificationService {
        -ExecutorService executor
        +sendBookingConfirmation(Booking)
        +sendCancellation(Booking)
    }
    class NotificationTask { +run() }
    NotificationService ..> NotificationTask : submits
    MockPaymentService ..> NotificationService
    BookingService ..> NotificationService
```

## 6.2 Use case diagram
```mermaid
flowchart LR
    P([Player])
    O([Venue Owner])
    A([Admin])
    subgraph QuickCourt
        UC1(Register / Login)
        UC2(Search and filter venues)
        UC3(View venue, courts, slots)
        UC4(Book court and slot)
        UC5(Pay - simulated)
        UC6(View booking history)
        UC7(Cancel booking)
        UC8(Export bookings CSV)
        UC9(Add / edit venue)
        UC10(Manage courts)
        UC11(Block slots for maintenance)
        UC12(View bookings and statistics)
        UC13(Approve / reject venues)
        UC14(Manage users - ban/unban)
        UC15(View platform statistics and report)
    end
    P --> UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC7 & UC8
    O --> UC1 & UC9 & UC10 & UC11 & UC12 & UC8
    A --> UC1 & UC13 & UC14 & UC15 & UC8
    UC4 -. includes .-> UC5
    UC4 -. includes .-> UC3
```

## 6.3 Architecture diagram
```mermaid
flowchart TB
    subgraph Browser
        FE["HTML / CSS / JS / Bootstrap 5<br/>(served from /static)"]
    end
    subgraph "Spring Boot application (Java 17)"
        INT["AuthInterceptor<br/>token + role check"]
        C["Controllers<br/>REST /api/**"]
        S["Services<br/>business rules"]
        R["Repositories<br/>Spring Data JPA"]
        T["NotificationService<br/>background threads"]
        F["Files: logs/ exports/ reports/"]
    end
    DB[("MySQL 8")]
    FE -- "HTTP + JSON" --> INT --> C --> S --> R --> DB
    S --> T --> F
    S --> F
```

## 6.4 Sequence diagram — booking and payment
```mermaid
sequenceDiagram
    actor Player
    participant UI as Browser (booking.js)
    participant BC as BookingController
    participant BS as BookingService
    participant DB as MySQL
    participant PC as PaymentController
    participant PS as MockPaymentService
    participant NS as NotificationService (thread)

    Player->>UI: choose court, date, slot
    UI->>BC: GET /venues/{id}/slots
    BC->>BS: getSlots()
    BS->>DB: bookings + blocked slots
    BS-->>UI: slots with availability
    Player->>UI: Confirm and pay
    UI->>BC: POST /bookings
    BC->>BS: createBooking() [synchronized]
    BS->>DB: slot already taken?
    alt slot taken
        BS-->>UI: 409 SlotUnavailableException
    else free
        BS->>DB: save booking (PENDING)
        BS-->>UI: 201 booking
        UI->>PC: POST /payments
        PC->>PS: processPayment()
        PS->>DB: save payment
        alt SUCCESS or CASH
            PS->>DB: booking = CONFIRMED
            PS-)NS: sendBookingConfirmation (async)
            NS-->>NS: write notifications.log
        else FAILED
            PS-->>UI: payment FAILED, booking stays PENDING
        end
        PS-->>UI: payment result
    end
```
