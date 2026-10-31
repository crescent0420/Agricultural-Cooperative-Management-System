<#
  rename-migrations-to-timestamps.ps1 - ONE-TIME change, keeps all your data.

  Renames  V1__baseline.sql ... V15__create_seasons_and_enrolment_windows.sql
  to       V<yyyyMMddHHmmss>__baseline.sql ...   (the date and time each one was applied)
  and updates Flyway's own history table to the new names, so Flyway still recognises them.

  Run from the project root, with the application STOPPED:
      scripts\rename-migrations-to-timestamps.bat

  What it does, in order:
    1. Checks that the migration folder has no uncommitted changes (so Git can undo the renames).
    2. Reads when each migration was applied from flyway_schema_history in your database.
    3. Shows the old and new names and asks you to type YES.
    4. Backs up the whole database (schema and data) to database\backups.
    5. Updates flyway_schema_history in ONE transaction (all rows or none).
    6. Renames the files with "git mv", so Git records them as renames.
    7. Empties the TEST database's schema, so the next test run rebuilds it with the new names.
#>
param(
    [string]$PgBin = "C:\Program Files\PostgreSQL\18\bin"
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "acms-env.ps1")

$db = Get-AcmsDb
$psql = Get-PgTool $PgBin "psql"
$pgDump = Get-PgTool $PgBin "pg_dump"
$migrationDir = Join-Path $AcmsProjectRoot "src\main\resources\db\migration"
$culture = [System.Globalization.CultureInfo]::InvariantCulture

# Old-style names have a short number (V1 ... V999999). Timestamp names have 14 digits and are left alone.
$oldFiles = @(Get-ChildItem $migrationDir -Filter "V*__*.sql" |
              Where-Object { $_.Name -match '^V(\d{1,6})__.+\.sql$' } |
              Sort-Object { [long]($_.Name -replace '^V(\d+)__.*$', '$1') })
if ($oldFiles.Count -eq 0) {
    Write-Host "Nothing to rename: every migration already has a timestamp name."
    exit 0
}

# 1. Git must be able to undo everything.
# (New, not-yet-committed files are allowed: they are not renamed.)
$dirty = git -C $AcmsProjectRoot status --porcelain --untracked-files=no -- src/main/resources/db/migration
if ($LASTEXITCODE -ne 0) { throw "git is not available or this is not a Git repository." }
if ($dirty) { throw "The migration folder has uncommitted changes. Commit them first:`n$dirty" }

$env:PGPASSWORD = $db.Password
try {
    # 2. When was each migration applied?
    $rows = & $psql -h $db.Host -p $db.Port -U $db.User -d $db.Name -X -At -F "|" -c `
        "SELECT version, to_char(installed_on, 'YYYYMMDDHH24MISS') FROM flyway_schema_history WHERE version IS NOT NULL AND success ORDER BY installed_rank"
    if ($LASTEXITCODE -ne 0) { throw "Could not read flyway_schema_history from database '$($db.Name)'." }
    $appliedAt = @{}
    foreach ($row in $rows) { $cols = $row.Split("|"); $appliedAt[$cols[0]] = $cols[1] }

    # New version = applied time (or the file's time if not applied yet), always strictly increasing.
    $plan = @()
    $previous = [datetime]::MinValue
    foreach ($file in $oldFiles) {
        $null = $file.Name -match '^V(\d+)__(.+)$'
        $oldVersion = $Matches[1]
        $rest = $Matches[2]
        $applied = $appliedAt.ContainsKey($oldVersion)
        $time = if ($applied) {
            [datetime]::ParseExact($appliedAt[$oldVersion], "yyyyMMddHHmmss", $culture)
        } else {
            [datetime]::ParseExact($file.LastWriteTime.ToString("yyyyMMddHHmmss"), "yyyyMMddHHmmss", $culture)
        }
        if ($time -le $previous) { $time = $previous.AddSeconds(1) }   # two applied in the same second
        $previous = $time
        $newVersion = $time.ToString("yyyyMMddHHmmss", $culture)
        $plan += [pscustomobject]@{
            OldName    = $file.Name
            NewName    = "V${newVersion}__$rest"
            OldVersion = $oldVersion
            NewVersion = $newVersion
            Applied    = $applied
        }
    }

    # Any migration that already has a timestamp name must come AFTER the renamed ones.
    $existing = @(Get-ChildItem $migrationDir -Filter "V*__*.sql" | Where-Object { $_.Name -match '^V(\d{14})__' } |
                  ForEach-Object { [long]($_.Name.Substring(1, 14)) })
    $lastNew = [long]$plan[-1].NewVersion
    foreach ($version in $existing) {
        if ($version -le $lastNew) {
            throw "Migration V$version already has a timestamp name but is older than the renamed ones. Remove it from the folder, run this script, then put it back."
        }
    }

    # 3. Show the plan and ask.
    $plan | Format-Table OldName, NewName, Applied -AutoSize | Out-String | Write-Host
    $answer = Read-Host "Rename these $($plan.Count) migrations and update Flyway's history in '$($db.Name)'? Type YES to continue"
    if ($answer -ne "YES") {
        Write-Host "Cancelled. Nothing was changed."
        exit 1
    }

    # 4. Backup first (contains personal data: database\backups is git-ignored).
    $backupDir = Join-Path $AcmsProjectRoot "database\backups"
    New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
    $backup = Join-Path $backupDir ("{0}-before-timestamp-rename-{1}.sql" -f $db.Name, (Get-Date -Format "yyyyMMdd-HHmmss"))
    & $pgDump -h $db.Host -p $db.Port -U $db.User -d $db.Name --no-owner --file $backup
    if ($LASTEXITCODE -ne 0) { throw "Backup failed. Nothing was changed." }
    Write-Host "Backup written to $backup"

    # 5. Update Flyway's history in one transaction. The file CONTENTS do not change,
    #    so the checksums Flyway stored stay valid.
    $updates = $plan | Where-Object { $_.Applied } | ForEach-Object {
        "UPDATE flyway_schema_history SET version = '$($_.NewVersion)', script = '$($_.NewName)' WHERE version = '$($_.OldVersion)';"
    }
    if ($updates) {
        $sqlFile = Join-Path ([System.IO.Path]::GetTempPath()) "acms-rename-flyway-history.sql"
        [System.IO.File]::WriteAllText($sqlFile, ($updates -join "`r`n") + "`r`n", $AcmsUtf8NoBom)
        & $psql -h $db.Host -p $db.Port -U $db.User -d $db.Name -X -q -v ON_ERROR_STOP=1 --single-transaction -f $sqlFile
        $code = $LASTEXITCODE
        Remove-Item $sqlFile -ErrorAction SilentlyContinue
        if ($code -ne 0) { throw "Updating Flyway's history failed and was rolled back. No file was renamed." }
        Write-Host "Flyway history updated in '$($db.Name)'."
    }

    # 6. Rename the files through Git.
    foreach ($step in $plan) {
        git -C $AcmsProjectRoot mv "src/main/resources/db/migration/$($step.OldName)" "src/main/resources/db/migration/$($step.NewName)"
        if ($LASTEXITCODE -ne 0) {
            throw "git mv failed for $($step.OldName). The database history is already updated: rename the remaining files by hand to the names in the table above."
        }
    }
    Write-Host "Files renamed."

    # 7. The test database is rebuilt from scratch by the next test run (its data is disposable).
    if ($db.TestName -and $db.TestName -ne $db.Name) {
        $exists = & $psql -h $db.Host -p $db.Port -U $db.User -d $db.Name -X -At -c "SELECT 1 FROM pg_database WHERE datname = '$($db.TestName)'"
        if ($exists -eq "1") {
            & $psql -h $db.Host -p $db.Port -U $db.User -d $db.TestName -X -q -v ON_ERROR_STOP=1 -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
            if ($LASTEXITCODE -eq 0) { Write-Host "Test database '$($db.TestName)' emptied; the next test run rebuilds it." }
            else { Write-Warning "Could not empty '$($db.TestName)'. Drop and recreate it before running the tests." }
        }
    }

    Write-Host ""
    Write-Host "Done. Next: start the application and check that Flyway reports 'Successfully validated'."
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
