# ACMS: Agricultural Cooperative Management System

A REST API for the Rwanda Cooperative Union. It follows a farmer from registration to the publication of the season's
quality grades, and keeps reliable agricultural and financial records on the way: crop programmes and enrolment,
input contributions and payments, field inspections and results, collection permits, and management reports.
There is no graphical interface; every function is an HTTP endpoint (Swagger UI and Postman collections included).

| | |
|---|---|
| Language and framework | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Mail) |
| Database | PostgreSQL 18, schema managed by Flyway (timestamped migrations); Hibernate only validates it |
| Security | JWT bearer tokens (Spring Security resource server), BCrypt passwords, a role-to-permission matrix |
| Tests | JUnit 5, Spring Boot Test with MockMvc, against a separate PostgreSQL test database |
| API documentation | OpenAPI / Swagger UI, Postman collections |
| Optional | Docker (the application and a Mailpit test mail server) |

---

## 1. Who uses it

| Role | What they do |
|---|---|
| **Farmer** | Views their own profile, enrols in crop programmes, sees their own published grades and contribution balance, and generates collection permits. |
| **Field Officer** | Works with the programme offerings assigned to them in their own district: sees the enrolled farmers, creates inspections and enters scores. |
| **District Agronomist** | Approves (or returns) and corrects the inspection results of their own district. |
| **Union Registrar** | Registers farmers and manages their status; manages cooperatives, crop programmes, seasons, offerings and enrolment windows across the whole Union. |
| **Finance Officer** | Records contribution obligations and payments; cannot change field or quality data. |
| **System Administrator** | Creates user accounts and assigns roles; reads the audit log. |

Field Officers and District Agronomists only see their own district. The full matrix of who may do what is in
[`docs/permission-matrix.md`](docs/permission-matrix.md); the running application also serves it at `GET /api/permissions`.

---

## 2. Requirements

* **Java 25** (for example Eclipse Temurin). Maven is not needed: the project includes the Maven wrapper (`mvnw`).
* **PostgreSQL 18** on your computer. The scripts expect `C:\Program Files\PostgreSQL\18\bin`; another folder can
  be given with `-PgBin "<folder>"`.
* Optional: **Docker Desktop** (see [`README.Docker.md`](README.Docker.md)) and **Postman**.

All commands below are for Windows PowerShell, run from the project folder (the folder that contains `pom.xml`).

---

## 3. Configuration (`.env`)

Every computer-specific or secret value lives in a `.env` file in the project folder. It is listed in `.gitignore`
and must **never** be committed. A real environment variable with the same name always wins over `.env`.

```powershell
Copy-Item .env.example .env      # then edit .env
```

| Key | Required | Meaning |
|---|---|---|
| `SERVER_PORT` | no (8081) | Port of the API |
| `DB_HOST`, `DB_PORT` | yes | PostgreSQL server, normally `localhost` and `5432` |
| `DB_NAME` | yes | Development database, for example `acms_db` |
| `DB_TEST_NAME` | no (`acms_test`) | Database used by the automated tests. It must end in `_test`: the tests refuse to run anywhere else. |
| `DB_USERNAME`, `DB_PASSWORD` | yes | The login the application uses (created by `scripts\create-database.bat`) |
| `ACMS_JWT_SECRET` | yes | Signing key for login tokens, at least 32 characters (see below) |
| `ACMS_JWT_ISSUER` | no (`acms-api`) | Issuer written into the tokens |
| `ACMS_JWT_TTL_MINUTES` | no (30) | How long a login token stays valid |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_FROM` | no | Outgoing email. Without them, mail goes to Mailpit on `localhost:1025`. For Gmail, use an app password. |
| `APP_BASE_URL` | no | Start of the links inside emails (email verification), default `http://localhost:8081` |
| `APP_DEMO_SEED` | no (`false`) | `true` = add the demo accounts and data at start-up, **only on an empty system** (section 6) |
| `DEMO_SEED_PASSWORD` | only with `APP_DEMO_SEED=true` | Password of every demo account, at least 12 characters. Never written to the log. |

Generate a random `ACMS_JWT_SECRET`:

```powershell
$bytes = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes); [Convert]::ToBase64String($bytes)
```

---

## 4. First-time setup and running

```powershell
# 1. Create the login and both databases (development and test) from the values in .env.
#    It asks for the password of the PostgreSQL superuser "postgres". Safe to run again.
scripts\create-database.bat

# 2. Start the API. On the first start, Flyway creates every table from the migrations.
.\mvnw spring-boot:run
```

The API then answers on `http://localhost:8081` (or your `SERVER_PORT`):

| Address | What |
|---|---|
| `http://localhost:8081/swagger-ui.html` | Swagger UI: every endpoint, with a **Authorize** button for the token |
| `http://localhost:8081/v3/api-docs` | The OpenAPI description (JSON) |
| `http://localhost:8025` | Mailpit inbox, when Mailpit runs (Docker), for the verification emails and one-time passwords |

**With Docker** (the API and Mailpit in containers, PostgreSQL stays on Windows):

```powershell
docker compose --progress plain up -d --build
docker compose logs app --tail 30        # expect "Started AcmsApplication"
```

Details in [`README.Docker.md`](README.Docker.md).

---

## 5. Using the API

1. **Log in:** `POST /api/auth/login` with `{"username": "...", "password": "..."}`. The answer contains
   `accessToken`.
2. Send it with every other request: `Authorization: Bearer <accessToken>`.
3. A new account must first replace its emailed one-time password: `POST /api/auth/change-password`. Until then the
   token holds no permission at all.

**Every answer has the same envelope**, success or error:

```json
{ "success": true,  "message": "OK", "data": { "...": "..." }, "timestamp": "2026-10-30T08:00:00Z" }
{ "success": false, "message": "Validation failed",
  "error": { "status": 400, "error": "Bad Request", "path": "/api/farmers", "fieldErrors": [ ... ] },
  "timestamp": "..." }
```

| Status | Meaning |
|---|---|
| 200 / 201 | Done / created |
| 400 | Invalid input (`fieldErrors` says which field) |
| 401 | Not logged in, or the token is invalid or expired |
| 403 | Logged in, but not allowed: wrong role, another district, or another farmer's record |
| 404 | The record does not exist |
| 409 | Conflict: a duplicate, or a change that would rewrite permanent history |
| 422 | Valid request that breaks a business rule (window closed, not enough paid, no places left...) |

Lists are paginated and sortable: `?page=0&size=20&sortBy=<field>&direction=asc|desc`; the answer's `data` holds
`content` and the page information.

---

## 6. User accounts

**Normal start (no seeded users).** The system starts with no users. The **first person who registers**
(`POST /api/auth/register`, then the email verification link) becomes the **System Administrator**. That administrator
creates every other account (`POST /api/users`); each new user receives a one-time password by email and must change
it at the first login.

**Demo accounts (seeded).** For a demonstration or a first look, ACMS can create one account per role, plus two
cooperatives and four farmers, at start-up. This happens **only on an empty system**, so real data is never touched,
and the next start does nothing.

```powershell
scripts\create-demo-database.bat          # an empty database "acms_demo" (asks for the postgres password)
```

Then, in `.env`: `DB_NAME=acms_demo`, `APP_DEMO_SEED=true`, `DEMO_SEED_PASSWORD=<at least 12 characters>`, and start
the API. Every demo account has the password `DEMO_SEED_PASSWORD`, a verified email and no forced password change.

| Username | Role | District | Notes |
|---|---|---|---|
| `demo.admin` | System Administrator | — | created first, so the first-user rule still holds |
| `demo.registrar` | Union Registrar | — | |
| `demo.finance` | Finance Officer | — | |
| `demo.officer` | Field Officer | Huye | |
| `demo.agronomist` | District Agronomist | Huye | |
| `demo.officer.musanze` | Field Officer | Musanze | shows that districts are kept apart |
| `demo.farmer` | Farmer | Huye | linked to the farmer *Aline Uwase* |
| `demo.farmer.musanze` | Farmer | Musanze | linked to the farmer *Eric Niyonzima* |

Seeded data: *Huye Maize Growers Cooperative* (Aline Uwase, Jean Habimana, Claudine Mukamana) and
*Musanze Potato Growers Cooperative* (Eric Niyonzima). The 5 provinces and 30 districts of Rwanda are always present
(read from `src/main/resources/seed/rwanda-districts.csv` at every start-up). Put `DB_NAME` back to your own database after the demonstration.

---

## 7. Automated tests

```powershell
.\mvnw clean test
```

The tests start the whole application against the **test database** (`DB_TEST_NAME`, default `acms_test`) with the
same login as in `.env`. They refuse to run against any database whose name does not end in `_test`, because some of
them empty tables. Emails are replaced by a mock, so nothing is sent.

| Test class | What it proves |
|---|---|
| `LoginAndTokenTest` | Login gives a token; a wrong password and an unknown user get the same 401; tampered and expired tokens are refused; unverified emails cannot log in |
| `RegistrationFlowTest` | Registration, email verification and forced password change; duplicates (409) and bad input (400); admin-created accounts and password resets |
| `FirstUserRegistrationTest` | Five people registering at the same moment on an empty system: exactly one becomes administrator (concurrency) |
| `AuthorizationRulesTest` | Function-level security: each role can only call its own endpoints |
| `PermissionMatrixTest` | The permission matrix itself: every permission has a role; separation of duties (money only for Finance, field data never for Finance) |
| `ResponseEnvelopeTest` | Successes and errors use the same envelope; the matrix and `/api/auth/me` endpoints |
| `OpenApiDocumentationTest` | Swagger documentation is complete: every endpoint has one of the 13 tags, a summary and a description; login, register and verify-email need no token; `docs/endpoints.md` lists every endpoint in both tables |
| `FarmerTest` | Registration with a unique membership number; duplicate national id (409) and validation (400); search by name, number, cooperative and status with paging and sorting; district scope; status changes (a retired farmer can never be reactivated, 422) and their audit |
| `FarmerStatusTest` | (unit) all 16 possible status changes: which are allowed and which are refused |
| `RwandaReferenceDataTest` | The 5 provinces and 30 districts are seeded, and districts cannot be created through the API |
| `SeasonAndWindowTest` | Seasons and enrolment windows: validation, open/close history, and two registrars opening the same window at once (concurrency) |
| `ProgrammeOfferingTest` | Offerings: business rules, validation, district scope, and what can no longer change once farmers are enrolled |
| `FieldOfficerLookupTest` | The Registrar's Field Officer list: enabled officers only, one district or all, sorted by username, no email; every other role 403 |
| `EnrolmentTest` | Every enrolment rule (window, status, prerequisites, land limit, district), and eight farmers competing for the last place: exactly one gets it (concurrency) |
| `ContributionPaymentTest` | Only confirmed payments count; a repeated payment request creates nothing new, even six at the same moment (idempotency, concurrency); no overpayment; history cannot be rewritten |
| `InspectionResultTest` | Weights total 100%; score limits; Draft → Submitted → Published with the grade; return for rework; authorised corrections keep the original; the database refuses direct changes; IDOR (403) |
| `CollectionPermitTest` | Permits only when fully paid, active and before distribution; one permit per enrolment even with five requests at once; every attempt recorded; verification |
| `ReportTest` | The four reports' numbers, totals and rounding, and that district staff see only their own district |
| `ReportMathTest` | (unit) the rounding rules of the reports, which match a farmer's own balance |
| `DemoDataSeederTest` | The demo accounts: only on an empty system, the admin first, all usable at once, never twice |
| `ReferenceDataSeederTest` | At start-up a missing district is added back from the data file, a district without a province gets its province, and a second run changes nothing |
| `ReferenceDataFileTest` | (unit) the data file lists the 30 districts in 5 provinces; a broken file stops the start-up with the line number |
| `EntityTimestampsTest` | `created_at` and `updated_at` are set on create, only `updated_at` moves on update, and every entity table has both |
| `ProjectStructureTest` | (unit) every entity extends `BaseEntity`, and every status or type enum is in `com.acms.enums` |

---

## 8. Postman collections

Folder `postman/`. Import them all, create an environment (for example **ACMS-Local**) and run each collection
**in this order**, with the environment selected:

| Collection | Covers |
|---|---|
| `ACMS.postman_collection.json` | Parts A–D: accounts and login, reference data, farmers, registration and email verification |
| `ACMS-Part-E-Enrolment` | Seasons, windows, offerings and enrolment rules |
| `ACMS-Part-F-Payments` | Contributions, payments, confirmation and retries |
| `ACMS-Part-G-Inspections` | Inspections, scores, approval, corrections |
| `ACMS-Part-H-Permits` | Collection permits, verification and the attempt history |
| `ACMS-Part-I-Reports` | The four management reports and their access rules |

Each collection contains successful **and** refused cases (400, 401, 403, 404, 409, 422), with tests on every request.

Environment variables (keep the passwords as type **secret**; the environment file is git-ignored):

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8081` |
| `registrarUsername` / `registrarPassword` | a Union Registrar (demo: `demo.registrar`) |
| `financeUsername` / `financePassword` | a Finance Officer (demo: `demo.finance`) |
| `officerUsername` / `officerPassword` | a Field Officer of your district (demo: `demo.officer`) |
| `agronomistUsername` / `agronomistPassword` | a District Agronomist of the same district (demo: `demo.agronomist`) |
| `farmerUsername` / `farmerPassword` | a farmer account (demo: `demo.farmer`) |
| `ownDistrictId` | the district of those staff accounts (demo: the id of Huye) |
| `ownFarmerId` | the farmer profile of the farmer account (demo: Aline Uwase's id) |

`officerUserId` and the ids of everything created during a run are set by the collections themselves.

---

## 9. Database

* **Migrations:** `src/main/resources/db/migration`, named `V<yyyyMMddHHmmss>__<description>.sql`. Create a new one
  with `scripts\new-migration.bat <description>`. A migration that has been applied is **never edited**; a change is
  always a new migration. Hibernate runs with `ddl-auto=validate`: it checks the tables but never changes them.
* **Reference data:** Rwanda's 5 provinces and 30 districts are listed in
  `src/main/resources/seed/rwanda-districts.csv`. At every start-up `ReferenceDataSeeder` adds whatever is missing
  and changes nothing that exists; to add or correct an entry, edit the file and restart.
* **Timestamps:** every entity extends `BaseEntity` (`common/`), which sets `created_at` and `updated_at`
  automatically; every entity table has both columns.
* **Rules in the database itself:** constraints, unique indexes and triggers protect what must never be lost:
  payments, contribution changes, published results and their corrections, permits and permit attempts cannot be
  deleted or rewritten, not even with direct SQL.
* **SQL export:** `scripts\export-sql.bat` writes the applied migrations and the current schema to
  `database/exports/` (timestamped files).
* **Older migration names:** `scripts\rename-migrations-to-timestamps.bat` converted the early numbered migrations to
  timestamps (kept for reference).

---

## 10. Project structure

```
src/main/java/com/acms
  common/       base classes (BaseEntity, BaseService, BaseController...), the response envelope, paging
  config/       security, JWT, OpenAPI, reference data and demo data seeders
  controller/   the REST endpoints
  dto/          request and response objects (entities never leave the services)
  entity/       JPA entities (all extend BaseEntity)
  enums/        statuses and types: FarmerStatus, PaymentStatus, ResultStatus, WindowStatus, Role...
  exception/    business exceptions, mapped to HTTP status codes in one handler
  repository/   Spring Data repositories and the report queries
  security/     the Permission list and the PermissionMatrix
  service/      business rules, district and ownership checks, transactions
src/main/resources/db/migration   Flyway migrations
src/main/resources/seed           reference data read at start-up (provinces and districts)
src/test/java/com/acms            automated tests
docs/                             permission matrix, reports
postman/                          API test collections
scripts/                          Windows helper scripts (.bat, each running a .ps1)
database/                         database creation script and SQL exports
```

## 11. Documentation

* [`docs/demo-plan.md`](docs/demo-plan.md): the 15-minute demonstration, step by step
* [`docs/endpoints.md`](docs/endpoints.md): every endpoint with its method, URI, purpose, roles, success and error codes
* [`docs/erd.md`](docs/erd.md): the entity-relationship diagram (images: `docs/erd-core.png`, `docs/erd-full.png`)
* [`docs/permission-matrix.md`](docs/permission-matrix.md): which role holds which permission
* [`docs/reports.md`](docs/reports.md): the data and the calculation behind each management report
* [`README.Docker.md`](README.Docker.md): running with Docker
