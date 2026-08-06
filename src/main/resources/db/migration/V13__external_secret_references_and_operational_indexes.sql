ALTER TABLE bank_connections
    ADD COLUMN IF NOT EXISTS credential_reference VARCHAR(255),
    ADD COLUMN IF NOT EXISTS credential_mode VARCHAR(30) NOT NULL DEFAULT 'INLINE_ENCRYPTED';

ALTER TABLE bank_connections
    DROP CONSTRAINT IF EXISTS ck_bank_connections_credential_mode;
ALTER TABLE bank_connections
    ADD CONSTRAINT ck_bank_connections_credential_mode
        CHECK (credential_mode IN ('INLINE_ENCRYPTED', 'EXTERNAL_REFERENCE', 'NONE'));

CREATE INDEX IF NOT EXISTS idx_outbox_events_status ON outbox_events(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_inbox_messages_status ON inbox_messages(status, updated_at);
CREATE INDEX IF NOT EXISTS idx_bank_submissions_status ON bank_submissions(status, next_status_poll_at);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_attempts_status ON webhook_delivery_attempts(status, next_retry_at);
