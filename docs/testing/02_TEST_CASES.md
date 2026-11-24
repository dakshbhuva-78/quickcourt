# Test Cases — QuickCourt (45 designed cases)

**How to use:** run the automated tests (`mvn test`) and the manual steps, then fill the **Actual result** and **Status** columns honestly (Pass / Fail). If a case fails, log a defect in `04_DEFECT_LOG_AND_REPORT_TEMPLATES.md`.
Techniques: **EP** equivalence partitioning · **BVA** boundary value analysis · **DT** decision table · **ST** state transition.
"Automated" shows the JUnit test that covers the case (U = unit, I = integration). Cases with "Manual" are executed in the browser/API tool.
Dates: `D+2` means "today + 2 days".

## A. Registration and login
| ID | Description | Technique | Input | Expected result | Actual | Status | Automated / Manual |
|---|---|---|---|---|---|---|---|
| TC-01 | Register a valid player | EP valid | name "Aarav", unique email, pw `Strong@123`, PLAYER | 201, account created, password stored as BCrypt hash | | | U `registersPlayerWithHashedPassword`, I `registerThenLoginSucceeds` |
| TC-02 | Register a valid venue owner | EP valid | role VENUE_OWNER | 201; user is a `VenueOwner` | | | U `registersVenueOwnerSubclass` |
| TC-03 | Duplicate email | EP invalid | email already registered | 409 "account already exists" | | | U `duplicateEmailIsRejected`, I `duplicateEmailGivesConflict` |
| TC-04 | Weak password (no upper-case / digit / < 8 chars) | EP + BVA | `abc`, `abcdefgh`, `Abcdefg1` (8 = valid boundary), `Abcdef1` (7 invalid) | `abc`, `abcdefgh`, `Abcdef1` → 400; `Abcdefg1` → 201 | | | I `weakPasswordIsRejected`; boundary cases Manual |
| TC-05 | Invalid email format | EP invalid | `aarav@`, `aarav.com` | 400 "Enter a valid email" | | | Manual (register form + API) |
| TC-06 | Name length boundary | BVA | 1 char / 2 chars / 60 chars / 61 chars | 1 → 400, 2 → ok, 60 → ok, 61 → 400 | | | Manual |
| TC-07 | Self-registration as ADMIN | DT | role = ADMIN via API | 400 "Admin accounts cannot be created…" | | | U `adminCannotSelfRegister` |
| TC-08 | Valid login | EP valid | correct email + password | 200 with token, role, dashboard | | | U `validLoginReturnsToken`, I `registerThenLoginSucceeds` |
| TC-09 | Login with wrong password | EP invalid | correct email, wrong pw | 401 "Invalid email or password" | | | U `wrongPasswordIsRejected`, I `wrongPasswordGivesUnauthorized` |
| TC-10 | Login with unknown email | EP invalid | unregistered email | 401, **same message** as TC-09 (no user enumeration) | | | U `unknownEmailGivesSameError` |
| TC-11 | Banned user logs in | DT | banned account | 403 "banned by the admin" | | | U `bannedUserCannotLogin`, I `bannedUserCannotLogin` |
| TC-12 | Protected API without token | DT | `POST /api/bookings` no header | 401 "Please log in first" | | | I `bookingWithoutLoginIsUnauthorized` |
| TC-13 | Player calls admin / owner API | DT | player token → `/api/admin/stats`, `/api/owner/venues` | 403 both | | | I `playerCannotOpenAdminApi`, `playerCannotOpenOwnerApi` |
| TC-14 | Password hash never exposed | Security | `GET /api/users/me` | response contains no `password` field | | | I `passwordHashIsNeverReturned` |

## B. Venue search and details
| ID | Description | Technique | Input | Expected result | Actual | Status | Automated / Manual |
|---|---|---|---|---|---|---|---|
| TC-15 | List all approved venues | EP | no filters | all APPROVED venues, sorted by name | | | U `searchWithoutFiltersReturnsAllSortedByName` |
| TC-16 | Filter by sport | EP | sport = BADMINTON | only badminton venues | | | U `searchBySport`, I `searchFindsApprovedVenueBySportAndLocation` |
| TC-17 | Filter by location (case-insensitive, partial) | EP | location = `BOPAL` | venues in Bopal | | | U `searchByLocationIgnoresCase` |
| TC-18 | Max price boundary | BVA | maxPrice = 400 (venue price exactly 400) | venue with 400 **included**; 401+ excluded | | | U `searchByMaxPriceIncludesBoundary` |
| TC-19 | Search with no match | EP | `q=zzz` | 200 with empty list; UI shows "No venues match" | | | U `searchWithNoMatchReturnsEmptyList`, I `searchWithUnknownNameReturnsEmptyList` |
| TC-20 | Pending/rejected venue hidden | DT | open `/venues/{pendingId}` | 404 | | | U `pendingVenueIsHiddenFromPlayers` |
| TC-21 | Non-existent venue id | EP invalid | `/venues/99999` | 404 "Venue not found" | | | U `unknownVenueThrows` |

## C. Booking
| ID | Description | Technique | Input | Expected result | Actual | Status | Automated / Manual |
|---|---|---|---|---|---|---|---|
| TC-22 | Book an available slot | EP valid | venue, court, D+2, `18:00-19:00` | 201, status PENDING, amount = venue hourly price | | | U `booksAnAvailableSlot`, I `fullBookingFlow_book_pay_list_cancel` |
| TC-23 | Book an already booked slot | EP invalid | same court/date/slot again | 409 "already booked" | | | U `rejectsAlreadyBookedSlot` |
| TC-24 | Duplicate booking by two players | DT | player A then player B same slot | A 201, B 409 | | | I `duplicateSlotBookingIsRejected` |
| TC-25 | 10 simultaneous requests, same slot | Concurrency | 10 players at once | exactly 1 × 201, 9 × 409 | | | I `ConcurrentBookingTest` |
| TC-26 | Booking in the past | BVA | yesterday | 400 "cannot be in the past" | | | U `rejectsPastDate`, I `pastDateIsRejected` |
| TC-27 | Advance-booking limit | BVA | D+90 valid, D+91 invalid | D+90 → 201; D+91 → 400 | | | U `rejectsTooFarInFuture` (D+120); boundary Manual |
| TC-28 | Slot outside opening hours / bad format | BVA | `23:00-24:00`, `evening`, `05:00-06:00` (opens 06:00) | 400 "Invalid time slot" | | | U `rejectsSlotOutsideOpeningHours`, `rejectsMalformedSlot` |
| TC-29 | Slot blocked for maintenance | DT | owner blocks 10:00-11:00; player books it | 409 "blocked for maintenance" | | | U `rejectsBlockedSlot`, I `ownerBlocksSlot_playerCannotBookIt` |
| TC-30 | Rebooking after cancellation | ST | A books, A cancels, B books same slot | B gets 201 | | | I `cancelledSlotCanBeBookedAgain` |
| TC-31 | Non-player tries to book | DT | owner/admin token | 403 "Only players can book" | | | U `onlyPlayersCanBook` |
| TC-32 | Slot list shows availability | EP | after booking 17:00-18:00 | 17:00-18:00 `available=false` (Booked); 18:00-19:00 `true` | | | I `bookedSlotShowsAsUnavailableInSlotList`, U `slotListMarksBookedAndFreeSlots` |

## D. Payment (simulated)
| ID | Description | Technique | Input | Expected result | Actual | Status | Automated / Manual |
|---|---|---|---|---|---|---|---|
| TC-33 | UPI payment succeeds | EP valid | `demo@upi` | payment SUCCESS, booking CONFIRMED, confirmation written to `logs/notifications.log` by a background thread | | | U `upiPaymentSucceeds`, I full flow |
| TC-34 | UPI payment fails / invalid id | DT | `fail@upi` ; `not-an-upi` | `fail@upi` → payment FAILED, booking stays PENDING; invalid id → 400 | | | U `upiIdStartingWithFailIsDeclined`, `invalidUpiIdIsRejected` |
| TC-35 | Card success, wrong length, declined | EP/BVA | `4111111111111111` ; 15 digits ; ends `0000` | SUCCESS ; 400 ; FAILED | | | U `cardPaymentSucceeds`, `cardWithWrongLengthIsRejected`, `cardEndingWithZerosIsDeclined` |
| TC-36 | Cash payment | DT | CASH | payment PENDING, booking CONFIRMED (pay at venue) | | | U `cashKeepsPaymentPendingButConfirmsBooking` |
| TC-37 | Retry after failed payment | ST | FAILED then CASH | second payment accepted, booking CONFIRMED | | | I `failedPaymentKeepsBookingPending` |
| TC-38 | Paying a cancelled/confirmed booking, or someone else's | DT | various | 400 / 400 / 403 | | | U `cancelledBookingCannotBePaid`, `alreadyConfirmedBookingCannotBePaidAgain`, I `cannotPayForAnotherPlayersBooking` |

## E. Cancellation, owner, admin
| ID | Description | Technique | Input | Expected result | Actual | Status | Automated / Manual |
|---|---|---|---|---|---|---|---|
| TC-39 | Cancel own future booking | ST | CONFIRMED booking D+3 | status CANCELLED, slot free again, cancellation notice logged | | | U `cancelsOwnFutureBooking`, I full flow |
| TC-40 | Cancel past / already cancelled / someone else's | DT | three cases | 400 / 400 / 403 | | | U `cannotCancelPastBooking`, `cannotCancelTwice`, `cannotCancelSomeoneElsesBooking` |
| TC-41 | Owner adds a venue | EP valid | valid venue data | 201, status PENDING, invisible to players | | | I `ownerAddsVenue_adminApproves_playerCanSeeIt`, U `newVenueIsPending` |
| TC-42 | Invalid venue data | BVA/EP | empty name; closing 07:00 before opening 20:00; price 0 | 400 each | | | I `invalidVenueDataIsRejected`, U `closingTimeBeforeOpeningIsRejected` |
| TC-43 | Owner edits someone else's venue | DT | other owner token | 403 | | | U `ownerCannotEditSomeoneElsesVenue` |
| TC-44 | Admin approves / rejects venue | DT | approve ; reject with comment | APPROVED → visible in search ; REJECTED + comment shown to owner | | | I `ownerAddsVenue_adminApproves…`, `adminCanRejectVenue`, U `adminApprovesVenueWithComment` |
| TC-45 | CSV export | EP | player exports bookings | `text/csv` with header; formulas in names neutralised | | | I `csvExportReturnsHeader`, U `CsvExporterTest` |

## F. System test scenarios (manual, in the browser)
| ID | Scenario | Steps | Expected result |
|---|---|---|---|
| SYS-01 | Player journey | Register → log in → search "badminton" → open venue → book `D+2 18:00` → pay by UPI `demo@upi` → My Bookings | Booking shows CONFIRMED; slot disabled for other players |
| SYS-02 | Failed payment recovery | Book → pay with `fail@upi` → see error → retry with Cash | Error message shown, booking confirmed after retry |
| SYS-03 | Owner onboarding | Register as owner → add venue → add 2 courts → admin approves → player books | Venue visible only after approval |
| SYS-04 | Maintenance block | Owner blocks slot → player opens booking page | Slot shown as "Maintenance", cannot be selected |
| SYS-05 | Admin moderation | Admin bans a player → player tries to log in | "banned by the admin" message |
| SYS-06 | Session expiry | Delete `qc_token` in browser storage → open My Bookings | Redirected to login |
| SYS-07 | Responsive layout | Open home, venues, booking at 375 px width | No horizontal scrolling, menu collapses |
| SYS-08 | Cloud smoke test | After deployment: open URL, `/actuator/health`, log in with demo user, make one booking | All succeed over HTTPS |

## G. Equivalence / boundary summary (for the report)
| Input | Valid classes | Invalid classes | Boundaries tested |
|---|---|---|---|
| Password | ≥8 chars with upper+lower+digit | too short; missing upper/lower/digit | 7 / 8 chars |
| Name | 2–60 chars | empty; 1 char; 61 chars | 1, 2, 60, 61 |
| Booking date | today … today+90 | past; > 90 days | yesterday, today, D+90, D+91 |
| Time slot | hourly slot inside opening hours | other format; outside hours | first and last slot of the day |
| Price filter | any non-negative number | negative / text | price equal to limit |
| Card number | 16 digits | any other length | 15, 16, 17 digits |
| Payment method | UPI / CARD / CASH | other value | – |

### Decision table — booking a slot
| Rule | Logged in as player | Venue approved | Date valid | Slot in hours | Slot free | **Result** |
|---|---|---|---|---|---|---|
| R1 | Y | Y | Y | Y | Y | **Booking created (PENDING)** |
| R2 | N | – | – | – | – | 401 / 403 |
| R3 | Y | N | – | – | – | 400 venue not open |
| R4 | Y | Y | N | – | – | 400 invalid date |
| R5 | Y | Y | Y | N | – | 400 invalid slot |
| R6 | Y | Y | Y | Y | N | 409 slot unavailable |
