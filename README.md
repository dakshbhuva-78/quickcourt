# QuickCourt — Sports Facility Booking Platform

> **Less Coordination. More Playing.**

QuickCourt lets players discover local sports venues, check live court availability, book a time slot, pay (simulated) and manage bookings. Venue owners manage venues, courts, blocked slots and see statistics. Admins approve venues and monitor the platform.

Semester 5 integrated project: **Startup Policies · E-Commerce · Java · Software Testing · Cloud Computing · Presentation Skills**.

> **Payments are simulated.** No real payment gateway, no real card data. All demo data is fake.

---

## 1. Tech stack

| Layer | Technology |
|---|---|
| Frontend | HTML5, CSS3, JavaScript, Bootstrap 5, Chart.js (charts) |
| Backend | Java 17, Spring Boot 3.3 (Web, Data JPA, Validation, Actuator) |
| Database | MySQL 8 (H2 in-memory for automated tests) |
| Testing | JUnit 5, Mockito, Spring Boot Test / MockMvc, JaCoCo coverage |
| Deployment | Docker, Docker Compose, GitHub Actions, AWS (guide included) |

> The browser loads Bootstrap, Chart.js and Google Fonts from a CDN, so the **frontend needs internet access**.

## 2. Quick start (pick ONE)

### Option A — Docker (easiest: no Java or MySQL install needed)
```bash
cp .env.example .env          # Windows: copy .env.example .env   (then edit the passwords)
docker compose up --build
```
Open **http://localhost:8080**

### Option B — Run from VS Code / terminal
1. Install **JDK 17+**, **Maven 3.9+**, **MySQL 8** (see `docs/01_SETUP_AND_RUN.md`).
2. Copy `src/main/resources/application-local.properties.example` to `application-local.properties` and put your MySQL password.
3. Run:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Open **http://localhost:8080** (the database `quickcourt` and all tables are created automatically, and demo data is inserted on first start).

### Demo accounts (created automatically)
| Role | Email | Password |
|---|---|---|
| Admin | admin@quickcourt.com | Admin@123 |
| Venue owner | owner1@quickcourt.com | Owner@123 |
| Player | player1@quickcourt.com | Player@123 |

Payment test tips: UPI `demo@upi` → success · UPI `fail@upi` → failed · card `4111111111111111` → success · card ending `0000` → failed · Cash → pay at venue.

## 3. Run the tests
```bash
mvn test                  # unit + integration tests (uses in-memory H2, no MySQL needed)
mvn verify                # also builds the JAR
```
Coverage report: open `target/site/jacoco/index.html`.

## 4. Project structure and purpose of every important file

```
quickcourt/
├── pom.xml                         Maven build file: dependencies + plugins (Spring Boot, JaCoCo)
├── Dockerfile                      Builds the app into a Docker image (2 stages)
├── docker-compose.yml              Runs MySQL + the app together
├── .env.example                    Template for passwords (copy to .env, never commit .env)
├── .github/workflows/ci-cd.yml     GitHub Actions: build, test, (optional) Docker push + deploy
├── database/                       schema.sql (reference) and useful_queries.sql (for the viva)
├── docs/                           All documentation (see section 6)
└── src/
    ├── main/java/com/quickcourt/
    │   ├── QuickCourtApplication.java   Starts the application
    │   ├── controller/                  REST API endpoints (the "front door" of the backend)
    │   │   ├── UserController.java        register, login, logout, profile
    │   │   ├── VenueController.java       public venue search/details/slots + all OWNER endpoints
    │   │   ├── BookingController.java     create / list / cancel booking, CSV export
    │   │   ├── PaymentController.java     simulated payment
    │   │   └── AdminController.java       users, venue approval, stats, report, logs
    │   ├── service/                     Business rules (the "brain")
    │   │   ├── UserService.java           registration, BCrypt hashing, login, ban/unban
    │   │   ├── VenueService.java          search, owner CRUD, blocked slots, admin review
    │   │   ├── BookingService.java        availability, duplicate-booking prevention, cancel
    │   │   ├── PaymentService.java        INTERFACE for payments
    │   │   ├── MockPaymentService.java    simulated implementation of PaymentService
    │   │   ├── NotificationService.java   background thread pool (multithreading)
    │   │   └── StatsService.java          numbers for dashboards
    │   ├── repository/                  Database access (Spring Data JPA interfaces)
    │   ├── model/                       Entities = database tables
    │   │   ├── User.java (abstract) → Player.java, VenueOwner.java, Admin.java   (inheritance)
    │   │   └── Venue, Court, Booking, Payment, BlockedSlot + enums
    │   ├── dto/                         Small classes (records) for request/response data
    │   ├── exception/                   Custom exceptions + GlobalExceptionHandler (JSON errors)
    │   ├── config/                      AuthInterceptor (login + role check), TokenStore, WebConfig, DataSeeder
    │   └── util/                        SlotUtil, TimeUtil, FileLogger, CsvExporter, ReportGenerator, NotificationTask
    ├── main/resources/
    │   ├── application.properties       Configuration (values come from environment variables)
    │   └── static/                      The whole frontend (served by Spring Boot)
    │       ├── index.html, login.html, register.html, venues.html, venue-details.html
    │       ├── booking.html, my-bookings.html, owner-dashboard.html, admin-dashboard.html
    │       ├── css/style.css            Brand colours and styling
    │       └── js/app.js (shared helpers), auth.js, venues.js, booking.js, dashboard.js
    └── test/                            JUnit tests (unit + integration)
```

## 5. Feature summary
- **Auth**: registration with password rules, BCrypt hashing, login/logout, token session, roles PLAYER / VENUE_OWNER / ADMIN, role-based API protection.
- **Player**: search & filter venues (sport, location, price, name), pagination, venue details, live slot availability, booking with amount, simulated payment (UPI/Card/Cash), booking history, cancel, CSV export, profile.
- **Owner**: add/edit venues (go to admin approval), add/remove courts, block maintenance slots, bookings list, KPIs + 3 charts.
- **Admin**: approve/reject venues with comment, user search/ban/unban, user booking history, platform KPIs + 5 charts, text report download, live log viewer.
- **Double-booking prevention** for the same court + date + slot (plus tests).

## 6. Documentation index
| File | Content |
|---|---|
| `docs/01_SETUP_AND_RUN.md` | Install, run, VS Code, how to change things, troubleshooting |
| `docs/02_GITHUB_GUIDE.md` | Push the project to GitHub step by step |
| `docs/03_API_DOCUMENTATION.md` | Every REST endpoint |
| `docs/04_JAVA_CONCEPTS.md` | OOP, inheritance, interface, exceptions, threads, files + viva Q&A |
| `docs/05_DATABASE_SCHEMA.md` | ER diagram, tables, sample data |
| `docs/06_UML_DIAGRAMS.md` | Class, use-case, architecture, sequence diagrams |
| `docs/testing/` | Strategy, plan, 45 test cases, non-functional tests, defect log, regression & summary templates |
| `docs/07_CLOUD_DEPLOYMENT.md` | Cloud architecture, AWS deployment, security, monitoring, backup, CI/CD, costs |
| `docs/business/BUSINESS_DOCUMENTATION.md` | Lean canvas, BMC, SWOT, PESTEL, revenue, compliance … |

## 7. Honest limitations (MVP)
- Payments are simulated; OTP verification, reviews/ratings, avatar upload and "open games / player matching" are **not** implemented (listed in the roadmap).
- Sessions are kept in memory (they reset when the server restarts and do not work across several server instances). For scaling, move to JWT or Redis.
- Hibernate `ddl-auto=update` is used for simplicity; production systems should use migrations (Flyway).
- Venue photos use sport illustrations unless an owner pastes an image link (no file upload).
- Business figures in `docs/business` are **assumptions**, not market research.
