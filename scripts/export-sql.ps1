<#
  export-sql.ps1 - writes every SQL command of the ACMS database into timestamped files.

  Run from the project root:
      powershell -ExecutionPolicy Bypass -File scripts\export-sql.ps1

  It produces two files in database\exports:
    acms-migrations-<timestamp>.sql  every Flyway migration in order, each stamped with
                                     when it was applied, by whom, and when the file last changed
    acms-schema-<timestamp>.sql      the complete current schema from pg_dump (no data)
#>
param(
    [string]$PgBin = "C:\Program Files\PostgreSQL\18\bin",
    [string]$OutDir = "database\exports"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not [System.IO.Path]::IsPathRooted($OutDir)) { $OutDir = Join-Path $projectRoot $OutDir }

# ---- 1. Read settings: a real environment variable wins, otherwise the value in .env ----
$settings = @{}
$envFile = Join-Path $projectRoot ".env"
if (Test-Path $envFile) {
    foreach ($line in [System.IO.File]::ReadAllLines($envFile)) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("#")) { continue }
        $parts = $trimmed.Split("=", 2)
        if ($parts.Count -eq 2) { $settings[$parts[0].Trim()] = $parts[1].Trim() }
    }
}
function Get-Setting([string]$name, [string]$default) {
    $fromEnvironment = [Environment]::GetEnvironmentVariable($name)
    if ($fromEnvironment) { return $fromEnvironment }
    if ($settings.ContainsKey($name)) { return $settings[$name] }
    return $default
}

$dbUrl = Get-Setting "DB_URL" "jdbc:postgresql://localhost:5432/acms_db"
if ($dbUrl -notmatch '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') { throw "Cannot read DB_URL: $dbUrl" }
$dbHost = $Matches[1]   # not $host: that name is reserved in PowerShell
$dbPort = if ($Matches[2]) { $Matches[2] } else { "5432" }
$dbName = $Matches[3]
$dbUser = Get-Setting "DB_USERNAME" "acms_user"
$dbPassword = Get-Setting "DB_PASSWORD" $null
if (-not $dbPassword) { throw "DB_PASSWORD is not set (in .env or the environment)." }

$psql   = Join-Path $PgBin "psql.exe"
$pgDump = Join-Path $PgBin "pg_dump.exe"
foreach ($tool in @($psql, $pgDump)) {
    if (-not (Test-Path $tool)) { throw "Not found: $tool. Pass -PgBin with your PostgreSQL bin folder." }
}

$now    = Get-Date
$stamp  = $now.ToString("yyyyMMdd-HHmmss")
$human  = $now.ToString("yyyy-MM-dd HH:mm:ss zzz")
$commit = "unknown"
try { $c = git -C $projectRoot rev-parse --short HEAD; if ($LASTEXITCODE -eq 0) { $commit = $c } } catch { }

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)   # psql can choke on a byte-order mark

$env:PGPASSWORD = $dbPassword   # only for this script's process; removed in "finally"
try {
    # ---- 2. When was each migration applied? Flyway records this itself ----
    $query = "SELECT script, to_char(installed_on, 'YYYY-MM-DD HH24:MI:SS'), installed_by, success " +
             "FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank"
    $rows = & $psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -X -At -F "|" -c $query
    if ($LASTEXITCODE -ne 0) { throw "psql failed (exit code $LASTEXITCODE)." }
    $history = @{}
    foreach ($row in $rows) { $cols = $row.Split("|"); $history[$cols[0]] = $cols }

    # ---- 3. File 1: every migration in the order Flyway applies them ----
    # Sort by version NUMBER: a plain text sort would put V10 before V2.
    $migrationDir = Join-Path $projectRoot "src\main\resources\db\migration"
    $files = Get-ChildItem $migrationDir -Filter "V*__*.sql" |
             Sort-Object { [int]($_.Name -replace '^V(\d+)__.*$', '$1') }

    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("-- ACMS: all migration SQL commands, in the order Flyway applies them")
    [void]$sb.AppendLine("-- Generated:  $human")
    [void]$sb.AppendLine("-- Database:   $dbName on ${dbHost}:$dbPort")
    [void]$sb.AppendLine("-- Git commit: $commit")
    [void]$sb.AppendLine("")
    foreach ($f in $files) {
        $h = $history[$f.Name]
        if ($h) {
            $applied = "applied $($h[1]) by $($h[2])"
            if ($h[3] -ne "t") { $applied += " (FAILED)" }
        } else {
            $applied = "NOT YET APPLIED to this database"
        }
        [void]$sb.AppendLine("-- ============================================================")
        [void]$sb.AppendLine("-- $($f.Name)")
        [void]$sb.AppendLine("--   file last changed: $($f.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))")
        [void]$sb.AppendLine("--   $applied")
        [void]$sb.AppendLine("-- ============================================================")
        [void]$sb.AppendLine([System.IO.File]::ReadAllText($f.FullName))
        [void]$sb.AppendLine("")
    }
    $migrationFile = Join-Path $OutDir "acms-migrations-$stamp.sql"
    [System.IO.File]::WriteAllText($migrationFile, $sb.ToString(), $utf8NoBom)

    # ---- 4. File 2: the complete current schema, as PostgreSQL itself describes it ----
    $schemaFile = Join-Path $OutDir "acms-schema-$stamp.sql"
    & $pgDump -h $dbHost -p $dbPort -U $dbUser -d $dbName --schema-only --no-owner --no-privileges --file $schemaFile
    if ($LASTEXITCODE -ne 0) { throw "pg_dump failed (exit code $LASTEXITCODE)." }
    $header = "-- ACMS database schema (pg_dump --schema-only, no data)`r`n" +
              "-- Generated:  $human`r`n-- Git commit: $commit`r`n`r`n"
    $body = [System.IO.File]::ReadAllText($schemaFile)
    [System.IO.File]::WriteAllText($schemaFile, $header + $body, $utf8NoBom)

    Write-Host "Wrote $migrationFile"
    Write-Host "Wrote $schemaFile"
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
