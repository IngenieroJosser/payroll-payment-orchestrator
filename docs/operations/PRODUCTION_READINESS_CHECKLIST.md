# Production readiness checklist

A release is not approved merely because the application starts. Every item below requires evidence linked to the release or change ticket.

## Build and supply chain

- [ ] `scripts/verify-enterprise.sh` or `.ps1` completes using the pinned Java 21 container.
- [ ] GitHub Actions `clean verify` passes on the exact commit.
- [ ] Unit, architecture, integration and migration tests pass.
- [ ] SBOM is generated and archived.
- [ ] Dependency, secret, source and container scans contain no unaccepted critical findings.
- [ ] The image release workflow rejected high/critical known vulnerabilities.
- [ ] The image is identified by immutable digest, signed by the release pipeline and has published build provenance.
- [ ] Flyway migrations were validated against a production-like staging snapshot.

## Identity, network and secrets

- [ ] `SPRING_PROFILES_ACTIVE=prod`.
- [ ] JWT, active/previous encryption and hash keys are independent and stored in the approved secret manager.
- [ ] Administrative bootstrap is disabled.
- [ ] mTLS subjects, client identities and IP allowlists were independently reviewed.
- [ ] Trusted proxies are explicit; forwarded headers are not accepted from arbitrary addresses.
- [ ] Outbound hosts contain only approved bank and webhook FQDNs.
- [ ] TLS truststores/certificates and rotation dates are documented.
- [ ] Database and RabbitMQ identities use least privilege and separate production credentials.

## Financial go-live gate

- [ ] `APP_BANK_ALLOW_UNCERTIFIED_PROVIDERS=false`.
- [ ] `APP_BANK_SANDBOX_FALLBACK_ENABLED=false`.
- [ ] `REST_GENERIC` and `SANDBOX` are absent from `APP_BANK_CERTIFIED_PROVIDERS`.
- [ ] Every enabled provider has a completed certification dossier.
- [ ] Duplicate submission, timeout-after-commit and partial-settlement scenarios passed.
- [ ] Maker-checker, limits and authorization policies were approved by the client.
- [ ] Reconciliation and return/reversal procedures were tested.
- [ ] `APP_PAYMENT_EXECUTION_ENABLED=true` is applied only in the approved go-live change.

## Reliability and operations

- [ ] Alert rules are loaded and routed to an on-call rotation.
- [ ] Dashboards cover API, PostgreSQL, RabbitMQ, outbox, inbox, bank submissions and webhooks.
- [ ] SLOs and incident severities are approved.
- [ ] PostgreSQL PITR is enabled and a restore drill met RPO/RTO.
- [ ] RabbitMQ recovery procedure was tested; queues are not treated as the financial ledger.
- [ ] Runbooks and escalation contacts are available to operators.
- [ ] Rollback preserves pending bank submissions, outbox and inbox records.
- [ ] Capacity/load tests cover expected payroll peaks plus agreed headroom.

## Privacy, audit and compliance

- [ ] Data classification and retention policies are approved.
- [ ] PII is masked in API responses, logs and webhook payloads.
- [ ] Audit events are exported to immutable or access-controlled retention storage.
- [ ] Access reviews and privileged action monitoring are enabled.
- [ ] Vulnerability disclosure and incident notification channels are defined.

Approval must include engineering, security, operations, product owner and the client/bank integration owner.
