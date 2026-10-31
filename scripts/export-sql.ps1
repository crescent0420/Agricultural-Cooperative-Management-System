<#
  export-sql.ps1 - writes every SQL command of the ACMS database into timestamped files.

  Easiest:   scripts\export-sql.bat        (double-click it, or type it in a terminal)
  Directly:  powershell -ExecutionPolicy Bypass -File scripts\export-sql.ps1

  Writes two files to database\exports:
    acms-migrations-<time>.sql  every Flyway migration in order, each stamped with when it was applied
    acms-schema-<time>.sql      the complete current schema from pg_dump (structure only, no data)
#>
param(
    [string]$PgBin = "C:\Program Files\PostgreSQL\18\bin",
    [string]$OutDir = "database\exports"
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "acms-env.ps1")
if (-not [System.IO.Path]::IsPathRooted($OutDir)) { $OutDir = Join-Path $AcmsProjectRoot $OutDir }

$db = Get-AcmsDb
$psql = Get-PgTool $PgBin "psql"
$pgDump = Get-PgTool $PgBin "pg_dump"

$now = Get-Date
$stamp = $now.ToString("yyyyMMdd-HHmmss")
$human = $now.ToString("yyyy-MM-dd HH:mm:ss zzz")
$commit = "unknown"
try { $c = git -C $AcmsProjectRoot rev-parse --short HEAD; if ($LASTEXITCODE -eq 0) { $commit = $c } } catch { }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$env:PGPASSWORD = $db.Password   # only for this script's process; removed in "finally"
try {
    # When was each migration applied? Flyway records this itself.
    $query = "SELECT script, to_char(installed_on, 'YYYY-MM-DD HH24:MI:SS'), installed_by, success " +
             "FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank"
    $rows = & $psql -h $db.Host -p $db.Port -U $db.User -d $db.Name -X -At -F "|" -c $query
    if ($LASTEXITCODE -ne 0) { throw "psql failed (exit code $LASTEXITCODE)." }
    $history = @{}
    foreach ($row in $rows) { $cols = $row.Split("|"); $history[$cols[0]] = $cols }

    # Sort by version NUMBER ([long]: timestamp versions have 14 digits, too big for [int]).
    $migrationDir = Join-Path $AcmsProjectRoot "src\main\resources\db\migration"
    $files = Get-ChildItem $migrationDir -Filter "V*__*.sql" |
             Sort-Object { [long]($_.Name -replace '^V(\d+)__.*$', '$1') }

    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("-- ACMS: all migration SQL commands, in the order Flyway applies them")
    [void]$sb.AppendLine("-- Generated:  $human")
    [void]$sb.AppendLine("-- Database:   $($db.Name) on $($db.Host):$($db.Port)")
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
    [System.IO.File]::WriteAllText($migrationFile, $sb.ToString(), $AcmsUtf8NoBom)

    $schemaFile = Join-Path $OutDir "acms-schema-$stamp.sql"
    & $pgDump -h $db.Host -p $db.Port -U $db.User -d $db.Name --schema-only --no-owner --no-privileges --file $schemaFile
    if ($LASTEXITCODE -ne 0) { throw "pg_dump failed (exit code $LASTEXITCODE)." }
    $header = "-- ACMS database schema (pg_dump --schema-only, no data)`r`n" +
              "-- Generated:  $human`r`n-- Git commit: $commit`r`n`r`n"
    [System.IO.File]::WriteAllText($schemaFile, $header + [System.IO.File]::ReadAllText($schemaFile), $AcmsUtf8NoBom)

    Write-Host "Wrote $migrationFile"
    Write-Host "Wrote $schemaFile"
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
