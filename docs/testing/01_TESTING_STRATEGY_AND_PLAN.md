# Testing Strategy and Test Plan — QuickCourt

## 1. Strategy
**Goal:** show with evidence that the main user journeys work, errors are handled, and defects are found, fixed and re-tested.
**Approach:** test pyramid — many fast unit tests, fewer integration tests, a few end-to-end (system) scenarios, plus non-functional checks.

| Level | What | How | Tool |
|---|---|---|---|
| Unit | One class at a time (rules in services, utilities); database mocked | Automated | JUnit 5 + Mockito (`src/test/java/.../service`, `util`) |
| Integration | Controller → service → repository → database, real HTTP-style calls | Automated | `@SpringBootTest` + MockMvc + H2 (`integration/`) |
| System | Complete user journeys through the browser against MySQL | Manual (scripted scenarios) | Browser, see section F of `02_TEST_CASES.md` |
| Acceptance | Real users try the main tasks | Manual | Usability checklist (`03_NON_FUNCTIONAL_TESTING.md`) |
| Non-functional | Performance, security, usability | Mixed | k6, checklist |
| Regression | Re-run all automated tests after each fix | Automated | `mvn test` + GitHub Actions |
| Post-deployment | Smoke test on the cloud URL | Manual/script | curl + browser |

**Test design techniques (black-box):** equivalence partitioning, boundary value analysis, decision tables, state-transition (booking status). **White-box thinking:** statement and branch coverage from JaCoCo.

## 2. Scope
**In scope:** registration/login, role-based access, venue search/filter, venue details, slots, booking, duplicate prevention, simulated payment, cancellation, owner venue/court/slot management, admin approval/ban, CSV export, statistics API.
**Out of scope:** real payment gateway, email/SMS delivery (simulated by a log file), OTP, cross-browser testing beyond Chrome + one more browser, penetration testing by professionals, load above free-tier limits.

## 3. Test environments
| Environment | Description |
|---|---|
| Automated | JDK 17, H2 in-memory DB (profile `test`), GitHub Actions `ubuntu-latest` |
| Local system test | MySQL 8 on the tester's laptop, Chrome |
| Cloud smoke test | Containers on the cloud VM (see cloud guide) |

## 4. Roles
| Role | Responsibility |
|---|---|
| QA lead | Owns this plan, test cases, defect log, summary report |
| Java lead | Unit tests for services, fixes defects |
| Frontend/e-commerce lead | UI scenarios, usability session |
| Cloud lead | Performance run, smoke test, CI pipeline |
| Product lead | Acceptance criteria, usability participants |

## 5. Entry and exit criteria
**Entry:** code compiles; application starts; demo data loaded; test cases reviewed.
**Exit:** 100% of *critical/high* test cases executed and passed (or defects documented); no open Critical/High defects; all automated tests green in CI; coverage of `service` package ≥ 70% line coverage (team target — report actual value honestly); performance threshold met or limits documented; test summary report completed.

## 6. Defect handling
Defects are logged in `04_DEFECT_LOG_AND_REPORT_TEMPLATES.md` with ID, severity, priority, steps, status, fix. **Failed tests and defects are never deleted** — after a fix, the test is re-run and the regression table updated.
Severity: Critical (crash/data loss/security) · High (main flow broken) · Medium (feature wrong, workaround exists) · Low (cosmetic).

## 7. Schedule (5-day project)
| Day | Testing activity |
|---|---|
| 1 | Review requirements, draft test cases |
| 2 | Write/run unit tests while coding services |
| 3 | Integration tests, first defect log entries |
| 4 | System tests in browser, security + usability checks, performance run |
| 5 | Regression run, cloud smoke test, test summary report |

## 7b. How to run and read the automated tests
```bash
mvn test
```
- Results: console summary and `target/surefire-reports/`.
- Coverage: `target/site/jacoco/index.html` → click package `com.quickcourt.service` → note line & branch coverage.
- **Reflection to write in the report:** which classes have high coverage (services, utils), which have low (config, controllers are covered indirectly by integration tests), which branches are still untested (e.g. IOException paths in file classes) and why that is acceptable or what you would add next.
