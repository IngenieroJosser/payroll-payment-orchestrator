-- Recoverable processing leases for idempotency and webhook delivery workers.

ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP WITH TIME ZONE;
UPDATE idempotency_keys SET locked_at = updated_at WHERE locked = TRUE AND locked_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_idempotency_locked_at
    ON idempotency_keys(locked, locked_at)
    WHERE locked = TRUE;

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_due_recovery
    ON webhook_delivery_attempts(status, next_retry_at, updated_at, created_at);
