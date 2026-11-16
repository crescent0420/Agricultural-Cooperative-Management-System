# Deploying ACMS (production)

This guide runs the complete system - the React app, the Spring Boot API and PostgreSQL - on one machine with
Docker, behind HTTPS, with real email. It was written for a Windows laptop (`https://localhost`); section 10 shows
what changes on a server with a domain name. Every command is PowerShell.

## 1. What runs where

```
Browser ──HTTPS 443──▶ web (Caddy)  ── /api/* ──▶ app (Spring Boot, port 8081 inside Docker) ──▶ db (PostgreSQL 18)
                        └─ every other path: the built React app (index.html for app routes)
```

| Container | Image | Reachable from | Data |
| --- | --- | --- | --- |
| `web` | built from `frontend/Dockerfile` (the app's production build + Caddy) | this machine, ports 80 and 443 (80 only redirects to HTTPS) | certificates in the `caddy-data` volume |
| `app` | built from `Dockerfile` (the API) | only `web` | none |
| `db` | `postgres:18-alpine` | only `app` | the `db-data` volume |

The app and the API share one address, so the browser never makes a cross-origin request and the backend needs no
CORS settings (URD gap G1). The files are in `deploy/`: `compose.yaml`, `Caddyfile`, `.env.example`, and `compose.test.yaml` for testing (section 7).

This production stack (Docker project `acms-prod`) is separate from development (`compose.yaml` at the project
root): its own containers, its own database in Docker, and real email. The two can run side by side.

## 2. Before you start

- **Docker Desktop** running (Settings > General: "Use the WSL 2 based engine").
- The project from GitHub, `develop` or `main`, in `D:\Java Files\ACMS\Agricultural-Cooperative-Management-System\ACMS`.
- **Ports 80 and 443 free.** Check; no output means free:
  ```powershell
  Get-NetTCPConnection -LocalPort 80,443 -State Listen -ErrorAction SilentlyContinue
  ```
  If something is listed, find it with `Get-Process -Id <OwningProcess>` and stop it (often IIS or another web server).
- **An email account that can send by SMTP.** For Gmail: turn on 2-Step Verification, then create an app password at
  Google Account > Security > 2-Step Verification > App passwords (name it "ACMS"). It is 16 letters; type it
  without spaces. Never put your normal Gmail password in `.env`.

## 3. First-time setup

Run everything from the `deploy` folder:

```powershell
Set-Location "D:\Java Files\ACMS\Agricultural-Cooperative-Management-System\ACMS\deploy"
Copy-Item .env.example .env
```

Create two random secrets, one for the database password and one for the login tokens. Each command prints a
48-character value; copy it into `.env` and do not keep it anywhere else:

```powershell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 48 | ForEach-Object { [char]$_ })
```

Edit the settings:

```powershell
notepad .env
```

| Setting | What to put |
| --- | --- |
| `SITE_ADDRESS` | `localhost` on the laptop |
| `DB_PASSWORD` | the first random value |
| `ACMS_JWT_SECRET` | the second random value |
| `MAIL_USERNAME`, `MAIL_FROM` | your Gmail address (`MAIL_FROM` keeps the form `ACMS <address>`) |
| `MAIL_PASSWORD` | the 16-letter app password |
| `APP_DEMO_SEED` | stays `false` |

`deploy/.env` is ignored by Git and never copied into an image. Keep it different from the development `..\.env`.

Build and start (the first build downloads Node, Maven, Java and PostgreSQL images and takes several minutes):

```powershell
docker compose up -d --build
docker compose ps
```

All three containers should be `Up`, `db` also `(healthy)`. The API is ready when its log says it started:

```powershell
docker compose logs app --tail 30
```

Look for `Started AcmsApplication`. On the first start Flyway creates every table and the official Rwanda districts
are loaded.

## 4. Trust the HTTPS certificate (laptop only)

For `localhost`, Caddy signs the certificate with its own local certificate authority. Tell Windows to trust that
authority once, or the browser warns "Your connection is not private":

```powershell
docker compose cp web:/data/caddy/pki/authorities/local/root.crt "$env:TEMP\acms-local-ca.crt"
Import-Certificate -FilePath "$env:TEMP\acms-local-ca.crt" -CertStoreLocation Cert:\CurrentUser\Root
Remove-Item "$env:TEMP\acms-local-ca.crt"
```

Windows asks whether to install the certificate: answer **Yes**. Close every browser window and open
https://localhost; the padlock shows a secure connection. (Firefox uses its own list: set
`security.enterprise_roots.enabled` to `true` in `about:config`.)

The authority lives in the `caddy-data` volume, so this is needed again only if that volume is deleted.

## 5. Create the first administrator

The system starts empty. The **first person who registers becomes the System Administrator**.

1. Open https://localhost/register and register with your own username and real email address.
2. The email arrives within a minute (check spam). It contains your one-time password and a verification link.
3. Open the verification link **on this laptop**: it points to `https://localhost`, which other devices cannot reach.
   The page shows a short confirmation (`"success": true`).
4. Open https://localhost/login, log in with the one-time password and choose your own password.
5. You are the System Administrator. Create the other accounts under **Users**; each person receives an email.

### New users on a laptop deployment

With `SITE_ADDRESS=localhost` the whole system, the email link included, works only on this laptop: on a phone or
another computer, `localhost` means that device, so the link does not load. New users therefore finish on the laptop:

1. The administrator creates the account under **Users**; the person receives the email wherever they read mail.
2. On the laptop, the person opens their own mailbox in the browser (e.g. Gmail on the web) and clicks the link
   there, then logs in at https://localhost/login with the one-time password and chooses their own password.
   Nobody else needs to see the email: the one-time password is only theirs.

The link still proves what it should: only someone who received the email has it, and the account cannot log in
until it is used (the login page says "Please confirm your email address first"). The administrator sees the result
in two places:

- **Users**: the account shows **Email verified**, or **Email not verified** in yellow while the link is unused.
- **Audit log**: an `EMAIL_VERIFIED` entry for the account when the link was used.

The link works once, for 24 hours. If it expired or the email never arrived, the administrator opens the account
and clicks **Reset password**: a new email goes out with a new one-time password and, while the address is still
unverified, a new link; the old link stops working. To let other devices use the links, run ACMS on a server
with a domain name (section 10).

## 6. Check the deployment

Tick each before calling the deployment done:

- [ ] https://localhost shows the login page with a padlock; http://localhost redirects to https.
- [ ] Logging in works; a page opened directly (e.g. https://localhost/seasons) loads after a refresh.
- [ ] An account created by the administrator receives its email.
- [ ] `docker compose ps` shows no ports for `app` and `db` (only `web` has 80 and 443).
- [ ] After `docker compose restart`, everything still works and the data is still there.

## 7. Test the deployed build

The 46 end-to-end tests (`frontend/e2e`) also run against the deployed images, so what was tested is exactly what
runs. They need the demo accounts and must read emails, which production has neither of, so they run on a
**throw-away copy** of production defined by `compose.test.yaml`: the same images, its own database with the demo
data, its own Mailpit, on https://localhost:8443. Production on 443 keeps running and is never touched.

Run this after every `docker compose up -d --build`, from `deploy`. The demo password is read from the development
`..\.env` (it is never printed):

```powershell
$env:DEMO_SEED_PASSWORD = (Select-String -Path ..\.env -Pattern '^DEMO_SEED_PASSWORD=(.*)$').Matches[0].Groups[1].Value.Trim()
docker compose -f compose.yaml -f compose.test.yaml up -d
Start-Sleep -Seconds 40
Set-Location ..\frontend
$env:E2E_BASE_URL = 'https://localhost:8443'
$env:E2E_API_URL = 'https://localhost:8443/api'
$env:E2E_MAILPIT_URL = 'http://localhost:8026'
npx playwright test
Set-Location ..\deploy
docker compose -f compose.yaml -f compose.test.yaml stop
Remove-Item Env:E2E_BASE_URL, Env:E2E_API_URL, Env:E2E_MAILPIT_URL, Env:DEMO_SEED_PASSWORD
```

Keep that order: every command that names `compose.test.yaml`, `stop` included, needs `$env:DEMO_SEED_PASSWORD`
set, and refuses to run without it.

Expected: `46 passed`. The report opens with `npm run e2e:report` (in `frontend`), and the test emails are at
http://localhost:8026 while the copy runs. The copy keeps its data between runs (the tests are written for that);
to remove it completely: `docker volume rm acms-prodtest_db-data acms-prodtest_caddy-data acms-prodtest_caddy-config`.
These names belong to the test copy only.

Always keep `-f compose.yaml -f compose.test.yaml` together in these commands: without the second file the command
acts on production.

## 8. Everyday operations

| Task | Command (in `deploy`) |
| --- | --- |
| Stop / start | `docker compose stop` / `docker compose start` |
| Status | `docker compose ps` |
| Logs | `docker compose logs app --tail 100` (also `web`, `db`; add `-f` to follow) |
| Update to the newest code | `git pull`, then `docker compose up -d --build` (Flyway updates the database by itself) |
| Remove the containers, keep the data | `docker compose down` |

**Never run `docker compose down -v` unless you mean it:** `-v` deletes the volumes, which means the whole
database and the certificates.

### Backup

```powershell
$file = "acms_prod_$(Get-Date -Format yyyyMMdd_HHmm).dump"
docker compose exec db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/backup.dump'
docker compose cp db:/tmp/backup.dump "..\database\backups\$file"
docker compose exec db rm /tmp/backup.dump
```

`database\backups` is ignored by Git. Copy the file somewhere outside the laptop too.

### Restore

This **replaces** all current data with the backup:

```powershell
docker compose stop app
docker compose cp "..\database\backups\<file>.dump" db:/tmp/restore.dump
docker compose exec db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists /tmp/restore.dump'
docker compose exec db rm /tmp/restore.dump
docker compose start app
```

## 9. Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| `web` will not start: "port is already allocated" | Another program uses 80 or 443 (section 2). |
| Browser: "Your connection is not private" | The local certificate is not trusted yet (section 4). |
| `app` keeps restarting | `docker compose logs app`: usually a missing `.env` value. |
| `app` log: password authentication failed | `DB_PASSWORD` was changed after the first start. PostgreSQL keeps the password it was created with; put the old one back. |
| No email arrives | `docker compose logs app` shows "Could not send the account email": with "535" the app password or address is wrong; otherwise check `MAIL_*`. The account still exists: the administrator can use **Reset password** to send a new email. |
| Verification link does not open | On a laptop it works only on the laptop itself (section 5, "New users on a laptop deployment"). |
| A change to `deploy/.env` has no effect | Recreate the containers: `docker compose up -d`. |

## 10. Moving to a server with a domain name

Same files, three differences:

1. Point a domain name (e.g. `acms.example.rw`) to the server and open ports 80 and 443 in its firewall.
2. In `deploy/.env`: `SITE_ADDRESS=acms.example.rw`. Caddy then fetches a free public certificate by itself;
   section 4 is not needed, and email links work from any device.
3. Once HTTPS works, add `Strict-Transport-Security "max-age=31536000"` to the `header` block in `Caddyfile`.

Back up the database on a schedule (section 8) and keep `deploy/.env` only on that server.

## 11. Security summary

- Secrets live only in `deploy/.env`: never committed, never in an image.
- Only Caddy is reachable from outside; the API and the database are on Docker's private network.
- HTTPS everywhere; plain HTTP only redirects. Caddy adds `X-Content-Type-Options`, `X-Frame-Options` and
  `Referrer-Policy` headers and hides its own name.
- Login tokens expire after 30 minutes; demo data is off; the audit log records the visitor's address
  (the API trusts Caddy's `X-Forwarded-For`).
