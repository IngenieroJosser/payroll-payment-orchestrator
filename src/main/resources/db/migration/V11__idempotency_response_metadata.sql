ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS response_status INTEGER;
ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS response_content_type VARCHAR(120);
ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;
UPDATE idempotency_keys SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE idempotency_keys ALTER COLUMN updated_at SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_idempotency_expiration ON idempotency_keys(expires_at);
