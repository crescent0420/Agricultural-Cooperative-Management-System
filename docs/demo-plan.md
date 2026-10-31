# ACMS demonstration plan

A 15-minute running order that shows every part of the brief on a clean system with the demo accounts, followed by
questions. Each step names what to show, the request, and the answer to point at.

## The day before

1. `.\mvnw clean test`: **126 tests, 0 failures**. Keep the window open with the result, or take a screenshot.
2. Create a clean demonstration database and switch to it:
   ```powershell
   scripts\create-demo-database.bat          # asks for the postgres password
   ```
   In `.env`: `DB_NAME=acms_demo`, `APP_DEMO_SEED=true`, `DEMO_SEED_PASSWORD=<at least 12 characters>`.
3. Start the system: `docker compose --progress plain up -d --build`, then
   `docker compose logs app --tail 30` must show **"Demo data created. Accounts …"** and **"Started AcmsApplication"**.
4. In Postman, duplicate your environment as **ACMS-Demo** and set the demo accounts (README section 8):
   `registrarUsername = demo.registrar`, `financeUsername = demo.finance`, `officerUsername = demo.officer`,
   `agronomistUsername = demo.agronomist`, `farmerUsername = demo.farmer`, every password = your `DEMO_SEED_PASSWORD`
   (type **secret**). `ownDistrictId` = the `districtId` that `GET /api/auth/me` shows for `demo.officer` (Huye);
   `ownFarmerId` = the `farmerId` that it shows for `demo.farmer` (Aline Uwase).
5. Rehearse once with Parts E to I. Then, for a clean start on the day, drop and recreate the demo database
   (`psql -U postgres -d postgres -c "DROP DATABASE acms_demo"`, then step 2 again) and restart the application.

Open before starting: Swagger UI (`http://localhost:8081/swagger-ui.html`), Postman with **ACMS-Demo** selected,
Mailpit (`http://localhost:8025`), a PowerShell window in the project folder, and `docs/erd-core.png`.

## Running order (about 15 minutes)

| Time | Brief | Show | Point at |
|---|---|---|---|
| 0:00 | (a) | The problem in one sentence, then `docs/erd-core.png` and the README role table | Six roles; 25 tables; one database; layered Spring Boot API |
| 1:00 | (a), (e) | Swagger UI, then `GET /api/permissions` | 92 endpoints; the permission matrix: who may do what, in one place |
| 2:00 | (e) | Log in as `demo.farmer`; `GET /api/farmers` | 403: function access refused |
| | (e) | `GET /api/farmers/{Eric's id}/results` with Aline's token | 403: the IDOR attempt is blocked |
| | (e) | Any request without a token, then with a token edited by one character | 401 both times |
| | (e) | As `demo.admin`: `GET /api/audit-logs` | The refused attempts are recorded, with who and when |
| 4:00 | (c) | As `demo.registrar`: `POST /api/farmers` | 201 with a new `RCU-…` membership number |
| | (c) | The same national id again | 409 duplicate |
| | (c) | `GET /api/farmers?sortBy=fullName&direction=desc&size=2` | Paging and sorting |
| | (c) | `PATCH /api/farmers/{id}/status` to RETIRED, then to ACTIVE | 200, then **422**: a retired farmer cannot come back |
| 6:00 | (d), (g) | Part E/F requests: window closed → enrol | 422 window not open |
| | (d), (g) | Open the window; contribution 1 000; pay 200 and confirm; enrol | 422: only 20% paid (30% needed) |
| | (d), (g) | Pay 200 more and confirm; enrol | 201: 40% is enough; enrolling again is 409 |
| | (d) | As `demo.officer`: the farmers of the offering | The officer sees their own offering's farmers |
| | (d) | Explain the last place (report section 4) and show `EnrolmentTest` in the test output | 8 farmers, 1 place: exactly one enrolment |
| 9:00 | (g) | Part F: send the same payment request twice (same `transactionReference`) | 201, then **200 with the same payment id**: no duplicate |
| 10:00 | (f) | Part G: inspections, scores, submit; approve as `demo.agronomist` | Grade calculated; published |
| | (f) | As `demo.officer.musanze`: enter scores on the Huye offering | 403: another district |
| | (f) | As `demo.officer`: change a score after publication | 409: only while Draft |
| | (f) | A correction by `demo.agronomist`; `GET /api/results/{id}/corrections` | Original and corrected values, reason, who, when |
| 12:00 | (g) | Part H: permit at 40% | 422, refused and recorded |
| | (g) | Pay the rest; permit; ask again; verify the number | 201, then 200 with the same number; `valid: true` |
| | (g) | `GET /api/permits/attempts` | Every attempt with its outcome |
| 13:30 | (h) | Part I: outstanding contributions as `demo.finance`, capacity as `demo.officer` | Totals equal the rows; the officer sees only their offering |
| 14:30 | (b) | In PowerShell: `psql -U <DB_USERNAME> -d acms_demo -c "DELETE FROM payments WHERE id = 1"` (a payment made at 6:00) | Refused by the database trigger: history cannot be deleted, even in SQL |
| 15:00 | (i) | The test result: 126 green | What each group proves (README section 7) |

## If something goes wrong

* **Docker does not start:** run `.\mvnw spring-boot:run` instead; everything else stays the same.
* **A Postman request fails:** show the same call in Swagger UI (Authorize with the token from the login answer).
* **A login is refused:** check the password of the demo accounts in `.env` (`DEMO_SEED_PASSWORD`); the demo data is
  only created on an empty database, so recreate `acms_demo` if needed.
* **No network:** nothing is needed; the system, the database and Mailpit all run on the laptop.

## Likely questions

| Question | Short answer |
|---|---|
| What if two farmers take the last place at once? | One conditional `UPDATE`; PostgreSQL makes the second wait and refuses it (409). Tested with 8 at once. |
| What if the server crashes during an enrolment? | One transaction: everything is rolled back, the place is returned. |
| How do you stop duplicate payments? | Unique `transactionReference`; a repeat returns the original payment (200). |
| Can an administrator change a payment in the database? | No: triggers refuse updates and deletes of financial history, even in SQL. |
| How does a district report match the Union report? | Same SQL with a filter, one snapshot, totals from the rows. |
| How are passwords stored? | BCrypt hashes; one-time passwords must be changed at first login. |
| What would you improve? | Report section 11: provider webhooks, login rate limiting, refresh tokens, CI with Testcontainers. |
