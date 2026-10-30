<#
  create-demo-database.ps1 - an EMPTY database for the demonstration (default: acms_demo), owned by the
  same login as in .env. Your development database (DB_NAME) is not touched. Safe to run again.

  Easiest:   scripts\create-demo-database.bat
  Directly:  powershell -ExecutionPolicy Bypass -File scripts\create-demo-database.ps1 -Name acms_demo

  Like create-database, it asks for the password of the PostgreSQL superuser "postgres" (only a superuser
  may create databases). Then, in .env:
      DB_NAME=acms_demo   APP_DEMO_SEED=true   DEMO_SEED_PASSWORD=<at least 12 characters>
  and start the application: Flyway creates the tables and DemoDataSeeder adds the demo accounts.
#>
param(
    [string]$Name = "acms_demo",
    [string]$PgBin = "C:\Program Files\PostgreSQL\18\bin",
    [string]$SuperUser = "postgres"
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "acms-env.ps1")

if ($Name -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw "Use a simple database name: lowercase letters, digits and _." }

$db = Get-AcmsDb
if ($Name -eq $db.Name -or $Name -eq $db.TestName) {
    throw "'$Name' is your development or test database. Choose another name for the demonstration."
}
$psql = Get-PgTool $PgBin "psql"

$sql = "SELECT format('CREATE DATABASE %I OWNER %I', :'demo_db', :'db_user') " +
       "WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'demo_db') \gexec"

Write-Host "Connecting to PostgreSQL on $($db.Host):$($db.Port) as '$SuperUser' (psql will ask for its password)."
$sql | & $psql -h $db.Host -p $db.Port -U $SuperUser -d postgres -X -v ON_ERROR_STOP=1 `
    -v demo_db=$Name -v db_user=$($db.User)
if ($LASTEXITCODE -ne 0) { throw "Creating the demo database failed (exit code $LASTEXITCODE)." }

Write-Host ""
Write-Host "Ready: database '$Name', owned by '$($db.User)' (if it already existed, it was left as it was)."
Write-Host "Next, in .env set:  DB_NAME=$Name   APP_DEMO_SEED=true   DEMO_SEED_PASSWORD=<12+ characters>"
Write-Host "then start the application. Put DB_NAME back to $($db.Name) after the demonstration."
