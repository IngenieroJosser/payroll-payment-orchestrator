#!/usr/bin/env sh
set -eu
: "${PGHOST:?PGHOST is required}"
: "${PGPORT:=5432}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${PGUSER:?PGUSER is required}"
: "${BACKUP_DIR:=./backups}"

mkdir -p "$BACKUP_DIR"
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
output="$BACKUP_DIR/payroll-${PGDATABASE}-${timestamp}.dump"
pg_dump --format=custom --compress=9 --no-owner --no-acl --file="$output" "$PGDATABASE"
sha256sum "$output" > "$output.sha256"
echo "Backup created: $output"
echo 'Encrypt and transfer the backup to immutable storage according to the organization retention policy.'
