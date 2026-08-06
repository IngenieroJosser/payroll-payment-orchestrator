# Changelog

## 1.1.0-enterprise-hardening - 2026-08-06

### Added
- Restricted/payment-enabled deployment modes with an explicit financial go-live gate.
- Fail-fast validation for mandatory, independent and non-placeholder cryptographic secrets.
- Bank provider certification allowlist and production prohibition of `SANDBOX`/`REST_GENERIC`.
- External bank credential references with environment/secret-manager resolution.
- AES-GCM key-rotation support with a previous-key migration window.
- IPv4/IPv6 CIDR matching for IP allowlists and trusted proxies.
- Operational backlog metrics, Prometheus alerts, SLO, backup/DR, incident and go-live runbooks.
- Hardened Kubernetes base and Azure Key Vault/Workload Identity overlay.
- Reproducible Java 21 Docker verification scripts and production preflight scripts.
- CodeQL and dependency-review security workflow.
- Signed release workflow with image scanning, GHCR publication, Cosign/OIDC signing and build-provenance attestation.
- Flyway V13 for external credential references and operational indexes.

### Changed
- Maven may run on Java 21 or later, while compilation and certified runtime remain pinned to Java 21.
- Staging and production start with payment execution disabled by default.
- Inline bank credentials are restricted to dev/test; enterprise environments use external references.

## 1.0.1 - 2026-08-06

### Fixed
- Replaced the module-specific `RabbitMQContainer` smoke-test dependency with Testcontainers core `GenericContainer`.
- Enforced Java 21 source, target and release metadata for Maven and Eclipse/JDT.
- Rebuilt `AuditLogServiceTest` against the current three-dependency `AuditLogService` constructor.
- Removed IDE null-analysis false positives caused by method references in the reported files.
- Enabled automatic Maven project refresh in VS Code.


## 1.0.0-production-foundation — 2026-08-06

### Added

- Multi-tenant actor context and resource authorization.
- Rich payroll state machine, maker-checker and state history.
- Multi-adapter `BankPaymentProvider` contract and provider registry.
- Bank submissions, per-payment results and status polling.
- Transactional outbox, inbox, retries and DLQ.
- Stronger audit, webhooks, reconciliation and idempotency metadata.
- Recoverable processing leases for idempotency keys and webhook deliveries.
- Prometheus/OpenTelemetry dependencies and financial metrics.
- ArchUnit, provider contract tests and PostgreSQL/RabbitMQ Testcontainers smoke test.
- Non-root container, SBOM and CI quality gate.
- Valid Springdoc OpenAPI 3.0.3 dependency for the Spring Boot 4 line.

### Changed

- Payroll is no longer marked paid immediately after submission.
- Sensitive account/document fields are masked in API responses.
- Production configuration is fail-fast and has no insecure secret defaults.
- Bank provider selection is dynamic per company/source account.

### Removed

- Obsolete `BankBatchRequest` and `BankBatchResponse` mockup contracts.
- Custom Jackson configuration superseded by Spring Boot auto-configuration.
