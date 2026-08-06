# Backup and disaster recovery

## Baseline objectives

The initial engineering target is RPO <= 5 minutes and RTO <= 60 minutes. The client must approve these values after a measured restore drill.

## PostgreSQL

- Enable point-in-time recovery with encrypted backups and transaction log/WAL retention.
- Store copies in a separate failure domain and immutable retention tier.
- Back up roles, extensions and configuration separately from application data.
- Run `ops/postgres/backup.sh` only with a least-privileged backup identity.
- Perform restore drills at least quarterly and before major schema changes.
- Record backup ID, checksum, restore duration, row counts and Flyway version.

## RabbitMQ

RabbitMQ is transport, not the authoritative financial ledger. Recovery order:

1. Stop payment workers and schedulers.
2. Restore PostgreSQL and validate Flyway schema history.
3. Inspect `bank_submissions` against bank evidence to identify transactions accepted after the restored point.
4. Reconcile outbox/inbox state without deleting records.
5. Restore RabbitMQ topology and credentials.
6. Resume polling first, then outbox publication, then new execution commands.

## Mandatory post-restore checks

- Tenant/company counts and foreign-key integrity.
- Outstanding outbox, inbox and webhook backlogs.
- Bank submissions in non-terminal states.
- Duplicate idempotency keys and execution IDs.
- Audit continuity around the recovery point.
- Reconciliation against the bank before enabling new payments.

Never mark a payroll as paid solely to resolve a restore discrepancy.
