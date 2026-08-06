-- Reliable bank submission lifecycle and at-least-once messaging controls.

CREATE TABLE bank_submissions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    batch_id UUID NOT NULL REFERENCES payroll_batches(id),
    bank_connection_id UUID NOT NULL REFERENCES bank_connections(id),
    execution_id UUID NOT NULL UNIQUE,
    provider_key VARCHAR(60) NOT NULL,
    bank_idempotency_key VARCHAR(180) NOT NULL,
    external_batch_id VARCHAR(180),
    status VARCHAR(40) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error_code VARCHAR(80),
    last_error_message VARCHAR(500),
    submitted_at TIMESTAMP WITH TIME ZONE,
    next_status_poll_at TIMESTAMP WITH TIME ZONE,
    last_status_check_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_bank_submission_provider_idempotency UNIQUE(provider_key, bank_idempotency_key),
    CONSTRAINT ck_bank_submission_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX idx_bank_submissions_batch ON bank_submissions(batch_id, created_at DESC);
CREATE INDEX idx_bank_submissions_poll ON bank_submissions(status, next_status_poll_at);
CREATE UNIQUE INDEX uk_bank_submissions_active_batch
    ON bank_submissions(batch_id)
    WHERE status IN ('PREPARED', 'SUBMITTING', 'ACCEPTED', 'PROCESSING', 'PARTIALLY_SETTLED');

CREATE TABLE bank_payment_results (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES bank_submissions(id) ON DELETE CASCADE,
    payment_id UUID NOT NULL REFERENCES payroll_payments(id),
    external_payment_id VARCHAR(180),
    external_status VARCHAR(80),
    normalized_status VARCHAR(40) NOT NULL,
    rejection_code VARCHAR(80),
    rejection_reason VARCHAR(500),
    settled_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_bank_payment_result_submission_payment UNIQUE(submission_id, payment_id)
);
CREATE INDEX idx_bank_payment_results_payment ON bank_payment_results(payment_id);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    event_version INTEGER NOT NULL,
    routing_key VARCHAR(160) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(128),
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_outbox_attempt_count CHECK (attempt_count >= 0)
);
CREATE INDEX idx_outbox_dispatch ON outbox_events(status, next_attempt_at, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);

CREATE TABLE inbox_messages (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL UNIQUE,
    message_type VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    correlation_id VARCHAR(128),
    payload_hash VARCHAR(128),
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_inbox_attempt_count CHECK (attempt_count >= 0)
);
CREATE INDEX idx_inbox_status_updated ON inbox_messages(status, updated_at);
