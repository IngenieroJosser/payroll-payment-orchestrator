# Service level objectives

These values are an initial engineering baseline, not a commercial promise. Each client contract must approve or replace them.

## Proposed SLOs

| Service indicator | Initial objective | Measurement |
|---|---:|---|
| Authenticated API availability | 99.90% monthly | Valid requests not returning platform 5xx/timeouts |
| Read API p95 latency | < 500 ms | Excludes client/network time and external bank calls |
| Command API p95 latency | < 750 ms | Measures durable acceptance, not bank settlement |
| Outbox publication delay | 99.9% under 60 s | `created_at` to `published_at` |
| Execution message processing | 99.9% under 120 s | Published event to inbox completion |
| Bank status polling freshness | 99% within configured poll interval + 60 s | Only when provider is reachable |
| Webhook delivery | 99% under 5 min | For endpoints returning successful responses |
| Audit persistence | 100% for protected financial transitions | Release-blocking invariant |

## Error budget

- Availability target 99.90% permits approximately 43 minutes of monthly unavailability.
- Exhausting 50% of the monthly budget pauses non-critical releases.
- Exhausting 100% pauses feature releases until a reliability review and corrective plan are approved.

## Dependency attribution

Bank outages, invalid customer endpoints and client network failures are measured separately, but the orchestrator remains responsible for durable state, controlled retry, reconciliation and transparent status.
