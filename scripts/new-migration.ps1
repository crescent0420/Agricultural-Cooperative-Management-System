<#
  new-migration.ps1 - creates an empty Flyway migration whose version is the current date and time.

  Easiest:   scripts\new-migration.bat create programme offerings
  Directly:  powershell -ExecutionPolicy Bypass -File scripts\new-migration.ps1 -Name "create programme offerings"

  Result:    src\main\resources\db\migration\V20261027143005__create_programme_offerings.sql
#>
param(
    [Parameter(Mandatory = $true)][string]$Name
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "acms-env.ps1")

# "Create Programme Offerings!" -> "create_programme_offerings"
$description = (($Name.Trim().ToLowerInvariant()) -replace '[^a-z0-9]+', '_').Trim('_')
if (-not $description) { throw "Give a short description, for example: create programme offerings" }

$migrationDir = Join-Path $AcmsProjectRoot "src\main\resources\db\migration"
$now = Get-Date
$version = $now.ToString("yyyyMMddHHmmss")
$fileName = "V${version}__$description.sql"
$path = Join-Path $migrationDir $fileName

# Safety: the new version must be later than every existing migration, or Flyway refuses to apply it.
$latest = Get-ChildItem $migrationDir -Filter "V*__*.sql" |
          ForEach-Object { [long]($_.Name -replace '^V(\d+)__.*$', '$1') } |
          Sort-Object -Descending | Select-Object -First 1
if ($latest -and [long]$version -le $latest) {
    throw "The newest migration ($latest) is not older than now ($version). Check your computer's clock."
}

$header = "-- $fileName`r`n" +
          "-- Created: $($now.ToString('yyyy-MM-dd HH:mm:ss zzz'))`r`n" +
          "-- Purpose: $($Name.Trim())`r`n" +
          "-- Never edit this file after it has been applied: add a new migration instead.`r`n`r`n"
[System.IO.File]::WriteAllText($path, $header, $AcmsUtf8NoBom)
Write-Host "Created $path"
