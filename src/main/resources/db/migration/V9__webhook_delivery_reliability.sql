ALTER TABLE webhook_delivery_attempts ADD COLUMN IF NOT EXISTS event_id UUID;
ALTER TABLE webhook_delivery_attempts ADD COLUMN IF NOT EXISTS payload TEXT;
ALTER TABLE webhook_delivery_attempts ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(128);
ALTER TABLE webhook_delivery_attempts ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE webhook_delivery_attempts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE webhook_delivery_attempts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
UPDATE webhook_delivery_attempts SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE webhook_delivery_attempts ALTER COLUMN updated_at SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_webhook_delivery_event_endpoint_attempt
    ON webhook_delivery_attempts(event_id, webhook_endpoint_id, attempt)
    WHERE event_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_endpoint_created
    ON webhook_delivery_attempts(webhook_endpoint_id, created_at DESC);
