# Enterprise readiness report

## Scope

This release hardens the existing Payroll Payment Orchestrator without replacing its financial domain, REST contracts, Flyway history, outbox/inbox model, multi-tenant authorization or bank-provider port.

## Enterprise deployment posture

The application now has two explicit operating modes:

1. **Restricted mode** — the default in staging and production. Tenant/company provisioning, configuration, payroll preparation, audit, reconciliation and operational validation remain available, while new monetary execution is blocked.
2. **Payment-enabled mode** — requires an approved provider allowlist, disabled sandbox/reference adapters, independent cryptographic secrets and an explicit go-live flag.

This makes the distribution safe to deploy to an enterprise environment before a bank connector is homologated. It does not misrepresent a generic connector as a bank-certified integration.

## Controls implemented

### Secrets and cryptography

- JWT, encryption and HMAC keys are mandatory; there are no empty or insecure runtime defaults.
- Startup rejects short, repeated, placeholder or known demonstration secrets.
- Active and previous encryption keys support controlled AES-GCM key rotation and dual-read migration.
- Bank credentials can be stored as external `env:<VARIABLE>` references; inline credentials are blocked outside dev/test.
- Azure Key Vault CSI/Workload Identity deployment examples are included.

### Financial go-live governance

- Payment execution is disabled by default in development, staging and production; tests enable it explicitly where required.
- `SANDBOX` is restricted to dev/test.
- `REST_GENERIC` is always prohibited in production.
- A production payment deployment must list at least one dedicated certified provider key.
- Provider resolution and bank-connection creation enforce the same governance policy.
- The bank certification checklist covers duplicate submissions, timeout-after-commit, partial settlement, reconciliation, certificate failures and contractual limits.

### Reproducible build

- Source and target bytecode remain fixed to Java 21.
- Maven can be launched by a later JDK, avoiding the reported Java 25 enforcer rejection, while CI and Docker remain pinned to Temurin 21.
- `scripts/verify-enterprise.sh` and `.ps1` execute the authoritative Java 21 container build.
- GitHub Actions run `clean verify`, integration tests, coverage, SBOM, dependency review and CodeQL.
- Tagged releases build the container, reject high/critical known vulnerabilities, push immutable tags, sign the digest with Cosign/OIDC and publish build provenance.

### Runtime and platform

- Production and staging profiles use fail-fast external configuration.
- Kubernetes manifests enforce non-root execution, read-only filesystem, dropped capabilities, seccomp, resource limits, rolling updates, PDB, HPA and health probes.
- Azure overlay demonstrates AKS Workload Identity plus Key Vault CSI secret injection.
- Outbound URL allowlisting and SSRF controls are strict outside dev/test.
- IP allowlists and trusted proxies support exact IPv4/IPv6 and CIDR rules.

### Operations

- Prometheus backlog gauges cover outbox, inbox, bank submissions and webhook deliveries.
- Alert rules cover availability, HTTP errors/latency, dead/retrying messages, failed submissions and failed webhooks.
- SLO/error-budget, backup/PITR, restore, disaster recovery, incident response, go-live and production-readiness procedures are included.
- PostgreSQL backup and restore scripts are supplied as operational references.

## Verification performed in the delivery environment

- 194 main Java source files compiled successfully with `javac --release 21`.
- 212 main `.class` files were generated.
- Custom executable harnesses validated:
  - IPv4/IPv6 CIDR matching and hostname rejection;
  - AES-GCM encryption/decryption and HMAC verification;
  - payment execution disabled/enabled policy;
  - production startup validation, placeholder rejection and generic-provider go-live rejection.
- XML, YAML and JSON files parsed successfully.
- Thirteen Flyway migrations have unique version identifiers.
- Shell scripts passed syntax validation.
- Production preflight passed in restricted mode and correctly rejected an invalid payment-enabled configuration.
- No generated classes, `.env`, private keys, certificates or build directories are included in the source distribution.

## External evidence still required before moving real money

The following cannot be created generically by source-code changes and must be completed per client and bank:

- Executed bank/API agreement and exact protocol specification.
- Dedicated provider adapter for that contract.
- Bank sandbox/homologation evidence and production credentials/certificates.
- Client-specific identity, approval limits, segregation-of-duties and fraud controls.
- Production-like load/capacity evidence.
- Penetration test and compliance/privacy review applicable to the jurisdiction and client.
- Restored-backup and disaster-recovery drill evidence.
- On-call routing, dashboards and change-management approvals.

Until those items are approved, deploy in restricted mode. Enabling `APP_PAYMENT_EXECUTION_ENABLED=true` is a controlled business and operational authorization, not merely a technical configuration change.

## Validation limitation

The full Maven/Testcontainers gate could not be executed in the delivery sandbox because Maven artifacts and Docker were unavailable there. The source code was compiled with Java 21 and the static/executable checks above passed. The repository includes deterministic Docker and GitHub Actions gates that must pass on the exact release commit before promotion.
