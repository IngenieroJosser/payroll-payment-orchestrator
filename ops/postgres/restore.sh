#!/usr/bin/env sh
set -eu
: "${1:?Usage: restore.sh <backup.dump>}"
: "${PGHOST:?PGHOST is required}"
: "${PGPORT:=5432}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${PGUSER:?PGUSER is required}"
backup=$1

[ -f "$backup" ] || { echo "Backup not found: $backup" >&2; exit 1; }
[ -f "$backup.sha256" ] && sha256sum --check "$backup.sha256"
pg_restore --clean --if-exists --no-owner --no-acl --exit-on-error --dbname="$PGDATABASE" "$backup"
echo 'Restore completed. Keep payment workers disabled until bank submissions and reconciliation evidence are verified.'
