# 3. REST API Documentation

Base URL: `http://localhost:8080/api` · Format: JSON · Dates `yyyy-MM-dd` · Times `HH:mm` · Slots `HH:mm-HH:mm`.

**Authentication:** after `POST /auth/login` send the returned token with every protected request:
`Authorization: Bearer <token>`.
**Access column:** Public = no login · Any = any logged-in user · PLAYER / OWNER / ADMIN = that role only.

**Error format** (all errors):
```json
{ "status": 409, "error": "Conflict", "message": "This slot is already booked. Please choose another slot.", "timestamp": "2026-11-21T10:15:30" }
```
| Status | Meaning in QuickCourt |
|---|---|
| 400 | Validation error, invalid date/slot, invalid payment input |
| 401 | Not logged in, wrong credentials, expired session |
| 403 | Wrong role, not your resource, banned account |
| 404 | Venue / court / booking / user not found |
| 409 | Slot unavailable, duplicate email |

## 3.1 Authentication & profile (`UserController`)
| Method & path | Access | Body | Success response |
|---|---|---|---|
| `POST /auth/register` | Public | `{name, email, password, role}` role = `PLAYER` or `VENUE_OWNER`. Password: 8+ chars, upper, lower, digit | `201 {message, id}` |
| `POST /auth/login` | Public | `{email, password}` | `200 {token, id, name, email, role, dashboard}` |
| `POST /auth/logout` | Any | – | `200 {message}` |
| `GET /users/me` | Any | – | user object (no password) |
| `PUT /users/me` | Any | `{name}` | updated user |

## 3.2 Venues (public) (`VenueController`)
| Method & path | Access | Notes |
|---|---|---|
| `GET /venues?sport=&location=&q=&maxPrice=` | Public | Only APPROVED venues. All parameters optional. `sport` ∈ BADMINTON, FOOTBALL, CRICKET, TENNIS, TABLE_TENNIS, BASKETBALL |
| `GET /venues/sports` | Public | List of sport types |
| `GET /venues/{id}` | Public | 404 if not approved |
| `GET /venues/{id}/courts` | Public | Active courts |
| `GET /venues/{id}/slots?courtId=&date=` | Public | `[{timeSlot, available, reason}]` reason = Booked / Maintenance / Time passed |

## 3.3 Bookings (`BookingController`)
| Method & path | Access | Body / params | Response |
|---|---|---|---|
| `POST /bookings` | PLAYER | `{venueId, courtId, bookingDate, timeSlot}` | `201` booking, status `PENDING` |
| `GET /bookings/my?status=` | Any | status optional | list of own bookings |
| `GET /bookings/{id}` | Booking owner / venue owner / admin | – | booking |
| `PUT /bookings/{id}/cancel` | Booking owner / admin | – | booking with status `CANCELLED` (future bookings only) |
| `GET /bookings/export` | Any | – | CSV file (player: own, owner: own venues, admin: all) |

Booking statuses: `PENDING` → `CONFIRMED` → `COMPLETED`, or `CANCELLED`.

## 3.4 Payments (`PaymentController`) — simulated
| Method & path | Access | Body | Response |
|---|---|---|---|
| `POST /payments` | PLAYER (own booking) | `{bookingId, paymentMethod, upiId?, cardNumber?}` | `201 {id, amount, paymentMethod, paymentStatus, transactionReference}` |
| `GET /payments/booking/{bookingId}` | Booking owner / venue owner / admin | – | latest payment |

Simulation rules: **UPI** id `name@bank`; starts with `fail` → FAILED, else SUCCESS · **CARD** 16 digits; ends with `0000` → FAILED, else SUCCESS · **CASH** → payment PENDING, booking CONFIRMED. SUCCESS/CASH confirm the booking; FAILED keeps it PENDING so the user can retry.

## 3.5 Venue owner area (`VenueController`, role VENUE_OWNER)
| Method & path | Body | Notes |
|---|---|---|
| `GET /owner/venues` | – | own venues, all statuses |
| `POST /owner/venues` | `{name, location, description, sportType, pricePerHour, openingTime, closingTime, amenities, imageUrl}` | `201`, status PENDING |
| `PUT /owner/venues/{id}` | same | rejected venues return to PENDING |
| `GET /owner/venues/{id}/courts` | – | |
| `POST /owner/venues/{id}/courts` | `{courtName, sportType}` | `201` |
| `PUT /owner/courts/{courtId}` | `{courtName, sportType}` | |
| `DELETE /owner/courts/{courtId}` | – | soft delete |
| `GET /owner/courts/{courtId}/blocks` | – | upcoming blocked slots |
| `POST /owner/courts/{courtId}/blocks` | `{date, timeSlot, reason}` | 409 if already booked/blocked |
| `DELETE /owner/blocks/{blockId}` | – | unblock |
| `GET /owner/bookings` | – | bookings of own venues |
| `GET /owner/stats` | – | KPIs and chart data |

## 3.6 Admin area (`AdminController`, role ADMIN)
| Method & path | Notes |
|---|---|
| `GET /admin/users?role=&q=` | search/filter users |
| `PUT /admin/users/{id}/ban` · `/unban` | admins cannot be banned |
| `GET /admin/users/{id}/bookings` | booking history of a user |
| `GET /admin/venues?status=` | PENDING / APPROVED / REJECTED |
| `GET /admin/venues/{id}/courts` | |
| `PUT /admin/venues/{id}/approve` · `/reject` | body `{comment}` optional |
| `GET /admin/bookings` | all bookings |
| `GET /admin/stats` | platform KPIs + chart data |
| `GET /admin/report` | text report download (also saved in `reports/`) |
| `GET /admin/logs?lines=30` | last lines of `logs/quickcourt.log` |

## 3.7 Monitoring (no login)
`GET /actuator/health` → `{"status":"UP"}` · `GET /actuator/metrics` · `GET /actuator/metrics/http.server.requests`
> In a real deployment, restrict `/actuator/metrics` (firewall or security group) — it is meant for the operator, not the public.

## 3.8 Example session (curl)
```bash
# login
curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d '{"email":"player1@quickcourt.com","password":"Player@123"}'
# search
curl "localhost:8080/api/venues?sport=BADMINTON&location=satellite"
# book (replace TOKEN)
curl -X POST localhost:8080/api/bookings -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" \
     -d '{"venueId":1,"courtId":1,"bookingDate":"2030-01-15","timeSlot":"18:00-19:00"}'
```
