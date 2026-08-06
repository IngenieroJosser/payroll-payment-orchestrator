# Incident response

## Severity

- **SEV-1:** suspected duplicate payment, unauthorized access, secret compromise, corrupted financial state, or platform-wide inability to process/track payments.
- **SEV-2:** one bank/tenant unavailable, growing outbox/DLQ backlog, reconciliation mismatch, or sustained SLO breach.
- **SEV-3:** isolated non-critical failure with a workaround and no financial integrity impact.

## Immediate containment for SEV-1

1. Set `APP_PAYMENT_EXECUTION_ENABLED=false` through the controlled deployment mechanism.
2. Do not delete outbox, inbox, submissions, audit logs or reconciliation evidence.
3. Preserve logs, traces, database snapshots and bank/provider responses.
4. Restrict compromised credentials and rotate through the secret manager.
5. Contact the bank integration owner using the approved channel.
6. Determine whether the external bank accepted each idempotency key before replaying anything.

## Communication

Every incident record must include start time, detection source, affected tenants, financial exposure, actions, decision owners and next update time. Client notification obligations are governed by contract and applicable regulation.

## Recovery approval

Payment execution resumes only after engineering, security and financial operations sign off on evidence that duplicate or unauthorized execution cannot occur.
