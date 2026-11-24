# Final Submission Checklist (maps the brief's 8 deliverables to files)

| # | Deliverable in the brief | Where it is | What YOU still must do |
|---|---|---|---|
| 1 | Topic proposal and startup brief | `docs/business/BUSINESS_DOCUMENTATION.md` §1–3 | Add survey/interview evidence; one-page proposal for mentor approval |
| 2 | E-commerce design document | `docs/business/BUSINESS_DOCUMENTATION.md` §9–13, 18–19 | Add screenshots of the platform flow |
| 3 | Java source code, UML diagram, README | `src/`, `docs/06_UML_DIAGRAMS.md`, `README.md`, `docs/04_JAVA_CONCEPTS.md` | Export UML diagrams as images; make sure every member can explain a part |
| 4 | Testing package | `docs/testing/` (strategy/plan, 45 test cases, non-functional, defect log, regression/summary templates), `src/test/`, `tests/performance/` | **Run** the tests, fill *Actual/Status* columns, JaCoCo coverage + reflection, real defects, k6 results, usability sessions |
| 5 | Cloud architecture document, Dockerfiles, CI/CD | `docs/07_CLOUD_DEPLOYMENT.md`, `Dockerfile`, `docker-compose.yml`, `deploy/`, `.github/workflows/ci-cd.yml` | **Deploy** on your cloud account; monitoring screenshots; real cost check |
| 6 | Working prototype running on the cloud | — | Public URL + smoke test (`deploy/smoke-test.sh`) |
| 7 | Pitch deck (10–12 slides) + funding & compliance note | `docs/presentation/QuickCourt_Pitch_Deck.pptx`, business doc §14–17 | Add team names (slide 1); update assumptions with your survey results |
| 8 | Final report + 5-minute demo video + viva | — | Compile report from the docs; record demo (see script below) |

## Rules from the brief — how this project follows them
- **Original work, credit libraries:** list Spring Boot, MySQL Connector/J, Bootstrap, Chart.js, Google Fonts, JUnit, Mockito, JaCoCo, H2, k6, Caddy in your report's references.
- **Payment gateways in test mode only:** payments are simulated.
- **Report test results honestly / defect log:** templates provided; keep failed tests.
- **No credentials in the repository:** env variables, `.gitignore`, `.env.example`.
- **Budget alert, shut down unused cloud resources:** cloud guide steps 1 and 10.
- **Regular commits from every member:** GitHub guide §2.5.

## 5-minute demo script
1. (0:00) Tagline + problem in 20 seconds.
2. (0:20) Home page → search badminton in Satellite → venue details with live slots.
3. (1:00) Log in as player → book a slot → pay with UPI → My Bookings → cancel one.
4. (2:00) Try booking the same slot with another browser/user → show the "already booked" message (duplicate prevention).
5. (2:30) Owner dashboard: KPIs, charts, add venue, block a slot.
6. (3:15) Admin: approve the venue, user management, charts, activity log.
7. (3:45) Show code: inheritance (`User`→`Player`…), `PaymentService` interface, custom exception, background thread log (`notifications.log`), CSV export.
8. (4:20) Show tests passing + coverage, GitHub Actions green, cloud URL + `/actuator/health`, CloudWatch graph.
9. (4:50) Close with business model and roadmap.

## Before you present — final self-check
- [ ] `mvn test` is green on your machine and in GitHub Actions
- [ ] You can start the app from scratch with Docker in one command
- [ ] Cloud URL works over HTTPS and demo accounts log in
- [ ] `git status` shows no `.env` / passwords
- [ ] Every team member can explain at least one module (controller, service, test, Docker, business model)
