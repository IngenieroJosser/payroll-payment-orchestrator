# Bank connector certification checklist

`REST_GENERIC` is a reference adapter and can never be declared certified. A production provider key must represent a dedicated adapter owned and versioned for a specific bank contract.

## Contract dossier

- [ ] Bank/API product, version, environment and owner identified.
- [ ] OpenAPI/WSDL/file layout and error catalogue archived.
- [ ] Authentication, mTLS, signing, encryption and certificate rotation documented.
- [ ] Idempotency semantics and duplicate-detection window confirmed in writing.
- [ ] Batch/payment status lifecycle mapped to internal normalized states.
- [ ] Cut-off times, holidays, currencies, limits and maximum batch size captured.
- [ ] Reconciliation, returns, reversals and charge handling defined.

## Adapter implementation

- [ ] Dedicated `BankPaymentProvider` implementation and provider key.
- [ ] No bank-specific DTOs leak into domain/application layers.
- [ ] Request signing and trust material are isolated in infrastructure.
- [ ] Timeouts, retryability and error codes are classified explicitly.
- [ ] Unknown bank states remain `UNKNOWN`; they are never treated as paid.
- [ ] PII and credentials are redacted from logs/traces.
- [ ] Metrics include provider, operation, normalized status and latency without high-cardinality IDs.

## Mandatory tests

- [ ] Successful submission and settlement.
- [ ] Validation rejection before acceptance.
- [ ] HTTP/network timeout before bank receipt.
- [ ] Timeout after bank receipt with safe idempotent retry.
- [ ] Duplicate idempotency key.
- [ ] Partial settlement and per-payment rejection.
- [ ] Delayed status, unknown status and provider outage.
- [ ] Invalid/expired certificate and invalid signature.
- [ ] Rate limiting and maintenance window.
- [ ] Reconciliation match, mismatch, return and duplicate statement/event.
- [ ] Load/capacity at the contractual batch limit.

## Evidence and approval

- [ ] Contract tests pass in CI.
- [ ] Bank sandbox/homologation evidence is attached.
- [ ] Non-monetary or controlled-value production validation completed when permitted.
- [ ] Security review and threat model approved.
- [ ] Operations runbook, contacts and certificate-expiry alerts configured.
- [ ] Provider key added to `APP_BANK_CERTIFIED_PROVIDERS` through a reviewed change.
