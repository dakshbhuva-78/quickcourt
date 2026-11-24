# 1. Setup, Run and Modify — QuickCourt

## 1.1 What to install
| Tool | Version | Why | Download |
|---|---|---|---|
| JDK (Java) | 17 or newer | Compile/run Java | Adoptium Temurin: https://adoptium.net |
| Maven | 3.9+ | Build tool (downloads libraries) | https://maven.apache.org/download.cgi |
| MySQL Server | 8.x | Database | https://dev.mysql.com/downloads/mysql/ |
| VS Code | latest | Editor | https://code.visualstudio.com |
| Git | latest | Version control | https://git-scm.com |
| Docker Desktop | optional | One-command run + cloud | https://www.docker.com/products/docker-desktop |

Check the installs (open a new terminal):
```bash
java -version      # should say 17 or higher
mvn -version
mysql --version
git --version
```
**Windows tip:** if `mvn` is "not recognised", add Maven's `bin` folder and `JAVA_HOME` to the PATH (search "Edit the system environment variables").

## 1.2 Open the project in VS Code
1. Unzip `QuickCourt.zip`.
2. VS Code → **File → Open Folder…** → choose the `quickcourt` folder (the one containing `pom.xml`).
3. Install these extensions when VS Code suggests them (or from the Extensions tab):
   - **Extension Pack for Java** (Microsoft)
   - **Spring Boot Extension Pack** (VMware)
   - *(optional)* **MySQL** / **SQLTools**, **Live Server is NOT needed** (Spring Boot serves the frontend).
4. Wait until the bottom-right shows Java/Maven loading is finished (first time downloads libraries; needs internet).

> IntelliJ IDEA Community also works: **Open** → select `pom.xml` → "Open as Project".

## 1.3 Set up MySQL
1. Make sure the MySQL service is running (Windows: *Services → MySQL80*; macOS: System Settings → MySQL).
2. Note your **root password** (set during MySQL installation).
3. You do **not** need to create the database or tables. The app creates database `quickcourt` and all tables automatically.
4. Copy `src/main/resources/application-local.properties.example` → `src/main/resources/application-local.properties` and edit:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```
This file is in `.gitignore`, so your password is never pushed to GitHub.

*(Alternative: set environment variables instead: `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`.)*

## 1.4 Run the application
**From the terminal (inside the project folder):**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
**From VS Code:** open `QuickCourtApplication.java` → click **Run** above `main`. To use your local profile, create `.vscode/launch.json` or add the VM argument `-Dspring.profiles.active=local`. (The terminal command above is simpler.)

When you see `Started QuickCourtApplication`, open **http://localhost:8080**.

Log in with the demo accounts from the README. Things to try:
1. Log in as **player1** → Venues → open "Green Arena Badminton Club" → Book now → choose court, date, slot → Confirm & pay.
2. Log in as **owner1** → Owner dashboard → add a venue (it becomes *PENDING*).
3. Log in as **admin** → Venue approvals → approve it → it now appears for players.

Health check (used in cloud monitoring): http://localhost:8080/actuator/health → `{"status":"UP"}`

## 1.5 Run with Docker instead
```bash
cp .env.example .env     # edit passwords inside .env
docker compose up --build
```
Stop with `Ctrl+C`; remove containers with `docker compose down` (add `-v` to also delete database data).

## 1.6 Run the tests
```bash
mvn test
```
Tests use an in-memory H2 database, so MySQL is not needed. Coverage report: `target/site/jacoco/index.html`.

## 1.7 Where to change things
| I want to… | Edit this file |
|---|---|
| Change brand colours / fonts | `src/main/resources/static/css/style.css` (variables at the top) |
| Change home page text | `static/index.html` |
| Add a sport (e.g. Squash) | `model/SportType.java` **and** the `SPORTS` object in `static/js/app.js` |
| Change booking rules (advance days, etc.) | `service/BookingService.java` (`MAX_ADVANCE_DAYS`) |
| Change payment simulation rules | `service/MockPaymentService.java` |
| Change demo data / demo passwords | `config/DataSeeder.java` (delete the DB or set `DDL_AUTO=create` to re-seed) |
| Change database / port | `application.properties` or environment variables |
| Add a new API endpoint | Controller → Service → Repository (same pattern as the existing ones) |
| Change slot length (1 hour → 30 min) | `util/SlotUtil.java` and `genSlots()` in `static/js/app.js` |

After editing **Java** files, stop and run again. After editing **HTML/CSS/JS** files you only need to refresh the browser (hard refresh `Ctrl+Shift+R`) when running from `mvn spring-boot:run`; if the change does not appear, restart the app.

**Reset the database (start clean):**
```sql
DROP DATABASE quickcourt;
```
then restart the app: tables and demo data are created again.

## 1.7b How a request flows (for explaining in the viva)
Browser (JavaScript `fetch`) → **Controller** (receives JSON, validates) → **Service** (business rules) → **Repository** (Spring Data JPA) → **MySQL**. Response goes back as JSON. Login uses a token that the browser sends in the `Authorization: Bearer …` header; the `AuthInterceptor` checks it and the user's role before any protected controller runs.

## 1.8 Troubleshooting
| Problem | Fix |
|---|---|
| `Access denied for user 'root'` | Wrong password in `application-local.properties` (or env var). |
| `Communications link failure` | MySQL is not running or wrong port. Start the MySQL service. |
| `Port 8080 was already in use` | Close the other program or run with `PORT=9090` (`mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090`). |
| `release version 17 not supported` | Your JDK is older than 17. Install JDK 17+ and check `java -version`. |
| `mvn` not recognised | Add Maven `bin` to PATH, or use the Maven view in VS Code. |
| Page looks unstyled / charts missing | No internet (Bootstrap/Chart.js load from a CDN). |
| "Please log in first" after restart | Sessions are in memory. Log in again. |
| Docker: `DB_PASSWORD` error | Create `.env` from `.env.example`. |
| Demo data missing | The seeder only runs on an empty database and when `SEED_DATA` is not `false`. |
