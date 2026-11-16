# End-to-end tests (Phase 6)

A real browser (Chromium, driven by Playwright) uses the frontend, which talks to the **real backend** in
Docker, on a **separate database `acms_e2e`** filled with the demo data. Your development database
(`acms_db`) is never touched.

| File | What it checks |
|---|---|
| `access.spec.js` (T2) | For every demo account: the menu shows exactly the allowed pages, each allowed page opens without a refused call, every other page says "Not available to you", and the backend answers **403** to calls the role has no permission for |
| `session.spec.js` (T3) | Login required, wrong password, tampered token → "session expired", logout, unknown page, 401 without or with an edited token, a farmer cannot read another farmer's results (403) |
| `workflows.spec.js` (T1) | The demo-plan workflows through the screens, checked in the backend: 1 accounts (emailed one-time password, linking a self-registered account); 2 farmers (membership number, duplicate national ID, retired stays retired); 3 enrolment (window, 30% rule, payment recorded and confirmed, 409 twice); 4 a payment sent twice is stored once; 5 scores → approval → grade, another district refused, correction history; 6 permit only at 100%, same number again, verified at the store; 7 report totals and the Field Officer's scope |
| `responsive.spec.js` (T4) | Phone (390 px), tablet (768 px) and desktop (1280 px): no page wider than the screen, the menu reachable; screenshots in `e2e-results/screens/` |
| `support/` | Logging in, calling the API, test data (`data.js`), reading emails from Mailpit, the demo accounts, reading `.env` |

## One-time setup

All commands in PowerShell.

1. In the **ACMS** folder, create the empty test database (asks for the `postgres` password):
   ```powershell
   scripts\create-demo-database.bat -Name acms_e2e
   ```
2. In `.env`, make sure `DEMO_SEED_PASSWORD` has at least 12 characters. The tests read it from there.
3. In the **frontend** folder, install Playwright and its browser (about 150 MB, once):
   ```powershell
   npm install --save-dev --save-exact @playwright/test@1.64.1
   npx playwright install chromium
   ```

## Running the tests

1. In the **ACMS** folder, start the backend on the test database (the override file switches only the
   database and turns the demo data on; `.env` is not changed):
   ```powershell
   docker compose -f compose.yaml -f compose.e2e.yaml up -d --build
   docker compose logs app --tail 30
   ```
   The first time, the log shows "Demo data created". Later runs reuse the same data.
2. In the **frontend** folder:
   ```powershell
   npm run e2e
   ```
   It builds the app and serves the production build on port 4173 by itself (your `npm run dev` on 5173
   can keep running). When a test fails, `npm run e2e:report` opens a report with a screenshot and a
   step-by-step trace of the failure.
3. Back to development, in the **ACMS** folder:
   ```powershell
   docker compose up -d
   ```

## Starting again from a clean database

Only when you want to (the tests do not need it, everything they create has a unique name):

```powershell
docker compose down
psql -U postgres -d postgres -c "DROP DATABASE acms_e2e"
scripts\create-demo-database.bat -Name acms_e2e
```

## Against the deployed build

The same tests run against the production images (HTTPS, Caddy, PostgreSQL in Docker) on a throw-away copy of
production: `docs/deployment.md`, section 7. They are pointed there with `E2E_BASE_URL`, `E2E_API_URL` and
`E2E_MAILPIT_URL`; with `E2E_BASE_URL` set, no local build or preview server is started.
