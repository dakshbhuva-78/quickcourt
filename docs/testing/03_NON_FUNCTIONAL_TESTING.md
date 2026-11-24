# Non-Functional Testing — Performance, Security, Usability

## 1. Performance / load testing approach
**Objective:** find out how many concurrent users the free-tier deployment handles and where it slows down.
**Tool:** k6 (script provided: `tests/performance/load-test.js`) — alternative: Apache JMeter.
**Scenario (read-heavy, realistic for browsing):** list venues → open courts → check slots → health check; ramp 1 → 10 → 50 virtual users over 80 s.
**Metrics to record:** requests/second, average and p95 response time, error rate, CPU and memory of the server (`docker stats` or cloud console), database connections.
**Run:**
```bash
k6 run tests/performance/load-test.js                          # local
k6 run -e BASE_URL=https://<your-domain> tests/performance/load-test.js   # cloud
```
**Acceptance targets (ASSUMED by the team — change if your faculty gives others):** error rate < 1 %, p95 response time < 800 ms at 50 virtual users on the free-tier VM.
**Second test — write path / concurrency:** `ConcurrentBookingTest` (10 simultaneous bookings for one slot → exactly one succeeds). Optionally extend the k6 script with login + booking of different slots to measure booking throughput.
**Report table (fill with real measurements):**
| Run | Users | Duration | Req/s | Avg ms | p95 ms | Errors % | CPU % | Memory MB | Verdict |
|---|---|---|---|---|---|---|---|---|---|
| Local | 10 | | | | | | | | |
| Local | 50 | | | | | | | | |
| Cloud | 50 | | | | | | | | |
**Typical findings to discuss:** the `/venues` endpoint loads all approved venues and filters in Java (fine for hundreds of venues; move filtering into SQL + pagination for thousands); connection pool size; JVM memory on a 1 GB VM.

## 2. Security testing checklist
Based on the OWASP Top 10. Tick **Pass/Fail** after testing and note evidence.

| # | Check | How to test | Control in QuickCourt | Result |
|---|---|---|---|---|
| S1 | Passwords stored hashed | Look at `users.password` in MySQL | BCrypt | |
| S2 | Password rules enforced server-side | API call with `abc` | `@Pattern` on `RegisterRequest` | |
| S3 | Wrong login does not reveal whether email exists | Compare messages | Same error text | |
| S4 | Protected APIs reject missing/invalid token | `curl` without header | `AuthInterceptor` → 401 | |
| S5 | Role-based access (broken access control) | Player token on `/api/admin/*`, `/api/owner/*` | 403 | |
| S6 | Object-level access (IDOR) | Player B pays/cancels/reads player A's booking id | ownership checks → 403 | |
| S7 | Owner cannot edit another owner's venue/court | Other owner's token | `ownedVenue` / `ownedCourt` → 403 | |
| S8 | SQL injection | Search `q=' OR '1'='1`; login email `a' OR 1=1 --` | JPA parameter binding, no string-built SQL | |
| S9 | Stored XSS | Register name `<script>alert(1)</script>`; venue name `<img src=x onerror=alert(1)>` | `esc()` escapes all server data before `innerHTML` | |
| S10 | Unsafe image URL | Venue image `javascript:alert(1)` | only http/https accepted | |
| S11 | CSV/Excel formula injection | Name `=HYPERLINK(...)` then export | `CsvExporter.escape` prefixes `'` | |
| S12 | Input validation / mass assignment | Send extra JSON fields such as `"role":"ADMIN"` on profile update | DTO records accept only expected fields | |
| S13 | No secrets in repository | `git grep -i password`, check `.env` ignored | env variables + `.gitignore` | |
| S14 | Banned user blocked even with old token | Ban, then call API with old token | interceptor checks `active` | |
| S15 | Error messages do not leak stack traces | Send malformed JSON | `GlobalExceptionHandler` | |
| S16 | HTTPS in production | Open `http://` URL | reverse proxy / load balancer (cloud guide) | |
| S17 | Actuator exposure | `/actuator/env` should be 404; metrics restricted in prod | only health/info/metrics exposed | |
| S18 | Session expiry | Wait > 24 h or restart server | token store expiry | |
**Known limitations to report honestly:** no rate limiting / account lockout on login (brute force possible); tokens are kept in `localStorage` (vulnerable if an XSS bug exists — mitigated by escaping); no CSRF token needed because tokens are sent in a header, not cookies; no email verification/OTP.

## 3. Usability testing checklist
**Method:** ask 5 real users (classmates/family) to complete the tasks without help; observe, time them, note where they hesitate. Do not collect personal data beyond first name and age group, and get verbal consent.
| Task | Success (Y/N) | Time (s) | Errors/hesitation | Comment |
|---|---|---|---|---|
| T1 Find a badminton venue in your area | | | | |
| T2 Check which slots are free tomorrow | | | | |
| T3 Book a slot and pay | | | | |
| T4 Find and cancel your booking | | | | |
| T5 (owner) Add a venue and a court | | | | |
**Heuristic checklist (Nielsen):**
- [ ] Clear navigation; current page highlighted
- [ ] Feedback after every action (toasts, disabled buttons while loading)
- [ ] Error messages explain what to do
- [ ] Booking summary and total always visible
- [ ] Consistent colours/buttons; sufficient contrast
- [ ] Works on a 375 px phone screen; tap targets are large enough
- [ ] Keyboard usable (court cards focusable; visible focus ring)
- [ ] Empty states explain next step ("No bookings yet. Find a venue…")
- [ ] No dark patterns: price shown before payment, no pre-ticked extras, easy cancellation
**Satisfaction question (1–5):** "It was easy to book a court." / "I would use this instead of calling the venue." — record the average.
