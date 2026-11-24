# Defect Log, Regression Report and Test Summary (templates)

> Rule from the brief: **report honestly. Never delete failed tests or hide defects.** A defect found, documented and fixed earns marks.

## 1. Defect log
| ID | Date | Found in (TC / area) | Description | Steps to reproduce | Severity | Priority | Status | Fix applied | Fixed by | Retest result |
|---|---|---|---|---|---|---|---|---|---|---|
| DEF-001 | (build) | Admin dashboard → Users tab | The "Active / Banned" status badge lost its colour: label was swapped with `String.replace()` which also changed the CSS class name | Open Admin → Users | Low | Low | **Fixed** (found in code review before first release) | Badge HTML written explicitly in `dashboard.js` | Java/Frontend lead | To be confirmed in SYS-05 |
| DEF-002 | (build) | Integration test class | Helper methods named `post/put/get` shadowed the statically imported MockMvc builders → compile error | Compile test sources | Medium | High | **Fixed** (found in code review) | Helpers renamed `doPost/doPut/doGet` | QA lead | Verify with `mvn test` |
| DEF-003 | (build) | Repositories | `select count(b) > 0 …` JPQL could fail on some Hibernate versions | Start app | Medium | High | **Fixed** (preventive) | Replaced with `count(...)` + Java `default` method | Java lead | Verify with `mvn test` |
| DEF-004 | | | *(add your real defects found during test execution here)* | | | | Open / Fixed / Rejected | | | |
> DEF-001–003 were found while reviewing the generated code, **before** any test run. Replace/extend with the defects *your team* really finds while executing the 45 test cases (for example UI problems on mobile, wrong messages, performance issues), keeping their original rows even after they are fixed.

**Status values:** New → Assigned → Fixed → Retested-Pass / Reopened → Closed.

## 2. Regression test report
Run after every fix (`mvn test`) and record the result; this is the evidence that earlier tests still pass.
| Regression run | Date | Trigger (fix / change) | Defects included | Tests run | Passed | Failed | New failures? | Notes / link to CI run |
|---|---|---|---|---|---|---|---|---|
| R1 | | Baseline after first complete build | – | | | | – | |
| R2 | | After fixing DEF-00x | DEF-00x | | | | | |
| R3 | | Before cloud deployment | – | | | | | |
| R4 | | After cloud config change | – | | | | | |
Attach screenshots of the GitHub Actions page (green run) and the Surefire/JaCoCo reports.

## 3. Test summary report
**Project:** QuickCourt · **Version:** 1.0 · **Report date:** ______ · **Prepared by:** ______

| Item | Planned | Executed | Passed | Failed | Blocked |
|---|---|---|---|---|---|
| Designed test cases (`02_TEST_CASES.md`) | 45 | | | | |
| Automated unit tests (`mvn test`) | | | | | |
| Automated integration tests | | | | | |
| System scenarios SYS-01…08 | 8 | | | | |
| Security checks S1…S18 | 18 | | | | |
| Usability tasks (5 users × 5 tasks) | 25 | | | | |
| Performance runs | 3 | | | | |
| Cloud smoke test | 1 | | | | |

**Code coverage (JaCoCo):** line ___ % · branch ___ % · `service` package line ___ %.
**Coverage reflection:** (what is well covered, what is not, why, what you would add)
**Defects:** found ___ · fixed ___ · open ___ (list open ones with severity).
**Known open issues / limitations:** (e.g. no login rate-limit, in-memory sessions)
**Performance result in one sentence:** ______
**Lessons learned:** (e.g. concurrency test showed … / the validation tests found … / what we would do earlier next time)
**Release recommendation:** ☐ Ready ☐ Ready with known issues ☐ Not ready — reason: ______
