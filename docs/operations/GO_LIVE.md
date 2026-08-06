# Controlled go-live

The application supports two operational modes:

- **Restricted mode:** `APP_PAYMENT_EXECUTION_ENABLED=false`. Companies can provision tenants, companies, accounts, roles, policies, webhooks and payroll drafts, but execution is blocked.
- **Payment mode:** `APP_PAYMENT_EXECUTION_ENABLED=true`. Startup validation requires a certified provider allowlist and rejects sandbox/reference providers.

## Promotion flow

1. Development uses `dev` with sandbox/reference providers.
2. Staging uses `staging`, production-like secrets and controlled homologation endpoints. Execution remains disabled unless a certification test explicitly requires it.
3. Production is deployed in restricted mode.
4. Bank certification evidence and readiness checklist are approved.
5. A change ticket sets the certified provider list and enables execution.
6. Operators validate one controlled payroll and reconciliation before broad rollout.

Disabling the execution flag is the first containment action for a suspected financial-integrity incident. It does not stop status polling or erase durable evidence.
