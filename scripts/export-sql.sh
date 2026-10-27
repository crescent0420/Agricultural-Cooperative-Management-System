#!/usr/bin/env bash
# export-sql.sh - writes every SQL command of the ACMS database into timestamped files.
# Same job as export-sql.ps1, for Git Bash, macOS or Linux.
#
# Run from anywhere:   bash scripts/export-sql.sh
# On Windows Git Bash, point it at PostgreSQL first:
#   PG_BIN="/c/Program Files/PostgreSQL/18/bin" bash scripts/export-sql.sh
set -euo pipefail
cd "$(dirname "$0")/.."

OUT_DIR="${OUT_DIR:-database/exports}"
MIGRATION_DIR="src/main/resources/db/migration"
PG_BIN="${PG_BIN:-}"

# Read a setting: a real environment variable wins, otherwise the value in .env.
# .env is only read, never executed.
get_setting() {
  local name="$1" default="$2" value="${!1:-}"
  if [[ -z "$value" && -f .env ]]; then
    value="$(grep -E "^[[:space:]]*${name}[[:space:]]*=" .env | tail -n 1 | cut -d= -f2- | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')" || true
  fi
  printf '%s' "${value:-$default}"
}

DB_HOST="$(get_setting DB_HOST '')"
DB_PORT="$(get_setting DB_PORT '')"
DB_NAME="$(get_setting DB_NAME '')"
# Older .env files had one DB_URL line instead of DB_HOST / DB_PORT / DB_NAME. Still understood.
DB_URL="$(get_setting DB_URL '')"
re='^jdbc:postgresql://([^:/]+)(:([0-9]+))?/([^?]+)'
if [[ -n "$DB_URL" && "$DB_URL" =~ $re ]]; then
  DB_HOST="${DB_HOST:-${BASH_REMATCH[1]}}"
  DB_PORT="${DB_PORT:-${BASH_REMATCH[3]:-5432}}"
  DB_NAME="${DB_NAME:-${BASH_REMATCH[4]}}"
fi
for v in DB_HOST DB_PORT DB_NAME; do
  if [[ -z "${!v}" ]]; then echo "$v is not set (add it to .env)." >&2; exit 1; fi
done
DB_USER="$(get_setting DB_USERNAME '')"
if [[ -z "$DB_USER" ]]; then echo "DB_USERNAME is not set (add it to .env)." >&2; exit 1; fi
PGPASSWORD="$(get_setting DB_PASSWORD '')"
if [[ -z "$PGPASSWORD" ]]; then
  echo "DB_PASSWORD is not set (in .env or the environment)." >&2; exit 1
fi
export PGPASSWORD

PSQL="${PG_BIN:+$PG_BIN/}psql"
PG_DUMP="${PG_BIN:+$PG_BIN/}pg_dump"

STAMP="$(date +%Y%m%d-%H%M%S)"
HUMAN="$(date '+%Y-%m-%d %H:%M:%S %z')"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
mkdir -p "$OUT_DIR"

# --- When was each migration applied? (Flyway records this itself) ---
HISTORY="$("$PSQL" -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -X -At -F '|' -c \
  "SELECT script, to_char(installed_on, 'YYYY-MM-DD HH24:MI:SS'), installed_by, success
   FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank" | tr -d '\r')"

# --- File 1: every migration, in the order Flyway applies them (V1, V2 ... V10, V11) ---
MIG_FILE="$OUT_DIR/acms-migrations-$STAMP.sql"
{
  echo "-- ACMS: all migration SQL commands, in the order Flyway applies them"
  echo "-- Generated:  $HUMAN"
  echo "-- Database:   $DB_NAME on $DB_HOST:$DB_PORT"
  echo "-- Git commit: $COMMIT"
  echo
  # Sort by version NUMBER: a plain text sort would put V10 before V2.
  for f in $(for p in "$MIGRATION_DIR"/V*__*.sql; do n="${p##*/V}"; echo "${n%%__*} $p"; done | sort -n | cut -d' ' -f2-); do
    name="${f##*/}"
    row="$(printf '%s\n' "$HISTORY" | awk -F'|' -v s="$name" '$1 == s')"
    if [[ -n "$row" ]]; then
      applied="applied $(cut -d'|' -f2 <<< "$row") by $(cut -d'|' -f3 <<< "$row")"
      [[ "$(cut -d'|' -f4 <<< "$row")" == "t" ]] || applied="$applied (FAILED)"
    else
      applied="NOT YET APPLIED to this database"
    fi
    changed="$(date -r "$f" '+%Y-%m-%d %H:%M:%S')"
    echo "-- ============================================================"
    echo "-- $name"
    echo "--   file last changed: $changed"
    echo "--   $applied"
    echo "-- ============================================================"
    tr -d '\r' < "$f"
    echo
    echo
  done
} > "$MIG_FILE"

# --- File 2: the complete current schema, as PostgreSQL itself describes it ---
SCHEMA_FILE="$OUT_DIR/acms-schema-$STAMP.sql"
TMP_FILE="$SCHEMA_FILE.tmp"
"$PG_DUMP" -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  --schema-only --no-owner --no-privileges --file "$TMP_FILE"
{
  echo "-- ACMS database schema (pg_dump --schema-only, no data)"
  echo "-- Generated:  $HUMAN"
  echo "-- Git commit: $COMMIT"
  echo
  cat "$TMP_FILE"
} > "$SCHEMA_FILE"
rm -f "$TMP_FILE"
unset PGPASSWORD

echo "Wrote $MIG_FILE"
echo "Wrote $SCHEMA_FILE"
