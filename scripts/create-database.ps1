<#
  create-database.ps1 - first-time setup on a new computer. Safe to run again: it skips what exists.

  Easiest:   scripts\create-database.bat
  It reads DB_USERNAME, DB_PASSWORD, DB_NAME and DB_TEST_NAME from .env, then asks for the password of
  the PostgreSQL superuser "postgres", and creates the login and both databases (main and test).
  Afterwards start the application once: Flyway creates every table from the migrations.
#>
param(
    [string]$PgBin = "C:\Program Files\PostgreSQL\18\bin",
    [string]$SuperUser = "postgres"
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "acms-env.ps1")

$db = Get-AcmsDb
$psql = Get-PgTool $PgBin "psql"
$sqlFile = Join-Path $AcmsProjectRoot "database\create-database.sql"

Write-Host "Connecting to PostgreSQL on $($db.Host):$($db.Port) as '$SuperUser' (psql will ask for its password)."
& $psql -h $db.Host -p $db.Port -U $SuperUser -d postgres -X -v ON_ERROR_STOP=1 `
    -v db_user=$($db.User) -v db_password=$($db.Password) -v db_name=$($db.Name) -v test_db_name=$($db.TestName) `
    -f $sqlFile
if ($LASTEXITCODE -ne 0) { throw "Database setup failed (exit code $LASTEXITCODE)." }
Write-Host "Ready: login '$($db.User)', databases '$($db.Name)' and '$($db.TestName)'."
