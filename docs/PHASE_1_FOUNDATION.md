# Phase 1 Foundation

## Scope

Phase 1 stabilizes the technical foundation without implementing the full banking roadmap. The project is aligned on Java 21 for local builds, Docker builds, and GitHub Actions.

## Fixed

- CI now uses JDK 21, matching `pom.xml` and the Dockerfile.
- `AuditLogEntity` maps only the columns currently created by Flyway for `audit_logs`: `actor`, `correlation_id`, and `client_ip`, plus the original audit columns.
- `AuditLogService` keeps the legacy `record(...)` API and adds operational context support for actor, correlation id, and client IP.
- Idempotency now records in-progress operations, validates repeated keys against the original request hash, stores response bodies, and rejects concurrent duplicate execution.
- Replayed idempotent responses are only returned when the repeated request uses the same request hash.
- `PayrollExecutionConsumer` keeps the current behavior but includes an explicit Phase 2 TODO for replacing the immediate `PAID` transition with bank confirmation.

## Not Changed

- No Flyway migrations were modified or added in this phase.
- No multi-bank adapter layer was implemented.
- No ISO 20022 message generation was implemented.
- No maker-checker workflow was implemented.
- No advanced reconciliation workflow was implemented.
- No asymmetric security model was implemented.

## Deferred Audit Fields

The current migrations do not create these `audit_logs` columns: `request_id`, `tenant_id`, `company_id`, `entity_type`, `entity_id`, `old_status`, `new_status`, `result`, or `failure_reason`.

They should be added only through an explicit future migration once the reporting and retention contract is defined. Until then they are intentionally not mapped, because `spring.jpa.hibernate.ddl-auto=validate` would fail startup if the entity declares columns that Flyway has not created.

## Why Multi-Bank Is Deferred

Multi-bank support requires a stable adapter contract, bank-specific error normalization, retry semantics, and integration tests per provider. Adding it now would expand the architecture beyond Phase 1 and increase the risk of breaking existing payroll endpoints.

## Why ISO 20022 Is Deferred

ISO 20022 needs message version selection, schema validation, bank-specific profile rules, and file/payment lifecycle handling. Phase 1 only stabilizes the base platform; ISO 20022 belongs after the state model and bank confirmation flow are corrected.

## Phase 2 Risks

- `PayrollExecutionConsumer` still marks batches as `PAID` immediately after send.
- The idempotency cache stores the response body but not the original HTTP status because there is no `status` column in the current schema.
- Audit has basic traceability, but tenant/company/state-result fields still need a migration and reporting contract.
- Distributed idempotency remains database-backed only; Redis or another distributed lock is still deferred.

## Recommended Phase 2

Replace the immediate `SENT_TO_BANK -> PAID` transition with a bank-confirmation state flow, persist the external bank batch id, add bank status polling or signed callbacks, and create the minimal migration needed for audit state transitions and idempotent HTTP status replay.
