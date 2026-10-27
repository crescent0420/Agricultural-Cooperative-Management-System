# acms-env.ps1 - shared helper, "dot-sourced" by the other scripts (". .\acms-env.ps1").
# Reads the project's .env file. A real environment variable with the same name always wins.
# Values are never printed.

$AcmsProjectRoot = Split-Path -Parent $PSScriptRoot
$AcmsSettings = @{}
$acmsEnvFile = Join-Path $AcmsProjectRoot ".env"
if (Test-Path $acmsEnvFile) {
    foreach ($line in [System.IO.File]::ReadAllLines($acmsEnvFile)) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("#")) { continue }
        $parts = $trimmed.Split("=", 2)
        if ($parts.Count -eq 2) { $AcmsSettings[$parts[0].Trim()] = $parts[1].Trim() }
    }
}

function Get-AcmsSetting([string]$Name, [string]$Default = $null) {
    $fromEnvironment = [Environment]::GetEnvironmentVariable($Name)
    if ($fromEnvironment) { return $fromEnvironment }
    if ($AcmsSettings.ContainsKey($Name) -and $AcmsSettings[$Name]) { return $AcmsSettings[$Name] }
    return $Default
}

# The database connection settings, all from .env.
function Get-AcmsDb {
    $db = [ordered]@{
        Host     = Get-AcmsSetting "DB_HOST"
        Port     = Get-AcmsSetting "DB_PORT"
        Name     = Get-AcmsSetting "DB_NAME"
        TestName = Get-AcmsSetting "DB_TEST_NAME" "acms_test"
        User     = Get-AcmsSetting "DB_USERNAME"
        Password = Get-AcmsSetting "DB_PASSWORD"
    }
    # Older .env files had one DB_URL line instead of DB_HOST / DB_PORT / DB_NAME. Still understood.
    $url = Get-AcmsSetting "DB_URL"
    if ($url -and $url -match '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') {
        if (-not $db.Host) { $db.Host = $Matches[1] }
        if (-not $db.Port) { $db.Port = if ($Matches[2]) { $Matches[2] } else { "5432" } }
        if (-not $db.Name) { $db.Name = $Matches[3] }
    }
    foreach ($key in @("Host", "Port", "Name", "User", "Password")) {
        if (-not $db[$key]) {
            $envName = @{ Host = "DB_HOST"; Port = "DB_PORT"; Name = "DB_NAME"; User = "DB_USERNAME"; Password = "DB_PASSWORD" }[$key]
            throw "$envName is not set. Add it to .env (see .env.example)."
        }
    }
    return $db
}

# Full path of a PostgreSQL program (psql, pg_dump), or a clear error.
function Get-PgTool([string]$PgBin, [string]$Tool) {
    $path = Join-Path $PgBin "$Tool.exe"
    if (-not (Test-Path $path)) {
        throw "Not found: $path. Run the script with -PgBin `"<your PostgreSQL bin folder>`"."
    }
    return $path
}

$AcmsUtf8NoBom = New-Object System.Text.UTF8Encoding($false)   # psql can choke on a byte-order mark
