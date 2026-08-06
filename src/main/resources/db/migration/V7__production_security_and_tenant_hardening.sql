-- Production hardening: tenant isolation, referential integrity, concurrency and audit context.

ALTER TABLE payroll_batches ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE payroll_batches pb
SET tenant_id = c.tenant_id
FROM companies c
WHERE pb.company_id = c.id
  AND pb.tenant_id IS NULL;
ALTER TABLE payroll_batches ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE payroll_batches ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payroll_batches ADD COLUMN IF NOT EXISTS created_by VARCHAR(160);
ALTER TABLE payroll_batches ADD COLUMN IF NOT EXISTS approved_by VARCHAR(160);
ALTER TABLE payroll_batches ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE payroll_batches ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

ALTER TABLE payroll_batches
    ADD CONSTRAINT fk_payroll_batches_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE payroll_batches
    ADD CONSTRAINT fk_payroll_batches_company
        FOREIGN KEY (company_id) REFERENCES companies(id);
ALTER TABLE payroll_batches
    ADD CONSTRAINT fk_payroll_batches_source_account
        FOREIGN KEY (source_account_id) REFERENCES bank_accounts(id);
ALTER TABLE payroll_batches
    ADD CONSTRAINT ck_payroll_batches_total_amount_positive CHECK (total_amount > 0);
ALTER TABLE payroll_batches
    ADD CONSTRAINT ck_payroll_batches_total_payments_positive CHECK (total_payments > 0);

ALTER TABLE payroll_payments
    ADD CONSTRAINT ck_payroll_payments_amount_positive CHECK (amount > 0);

ALTER TABLE users
    ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE users
    ADD CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies(id);

ALTER TABLE api_clients ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE api_clients ac
SET tenant_id = c.tenant_id
FROM companies c
WHERE ac.company_id = c.id
  AND ac.tenant_id IS NULL;
ALTER TABLE api_clients
    ADD CONSTRAINT fk_api_clients_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_clients
    ADD CONSTRAINT fk_api_clients_company FOREIGN KEY (company_id) REFERENCES companies(id);

ALTER TABLE bank_connections ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE bank_connections bc
SET tenant_id = c.tenant_id
FROM companies c
WHERE bc.company_id = c.id
  AND bc.tenant_id IS NULL;
ALTER TABLE bank_connections ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE bank_connections ADD COLUMN IF NOT EXISTS provider_key VARCHAR(60) NOT NULL DEFAULT 'REST_GENERIC';
ALTER TABLE bank_connections ADD COLUMN IF NOT EXISTS environment VARCHAR(30) NOT NULL DEFAULT 'PRODUCTION';
ALTER TABLE bank_connections ADD COLUMN IF NOT EXISTS connect_timeout_ms INTEGER NOT NULL DEFAULT 5000;
ALTER TABLE bank_connections ADD COLUMN IF NOT EXISTS read_timeout_ms INTEGER NOT NULL DEFAULT 30000;
ALTER TABLE bank_connections ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;
UPDATE bank_connections SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE bank_connections ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE bank_connections
    ADD CONSTRAINT fk_bank_connections_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE bank_connections
    ADD CONSTRAINT ck_bank_connections_timeouts CHECK (connect_timeout_ms BETWEEN 100 AND 60000 AND read_timeout_ms BETWEEN 100 AND 300000);
CREATE UNIQUE INDEX IF NOT EXISTS uk_bank_connections_active_company_bank
    ON bank_connections(company_id, bank_code)
    WHERE status = 'ACTIVE';

ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS company_id UUID;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS actor_type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS result VARCHAR(30) NOT NULL DEFAULT 'SUCCESS';
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS old_status VARCHAR(40);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS new_status VARCHAR(40);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS event_id UUID;
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created ON audit_logs(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_company_created ON audit_logs(company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_id ON audit_logs(event_id);

ALTER TABLE payroll_batch_status_history ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE payroll_batch_status_history ADD COLUMN IF NOT EXISTS company_id UUID;
ALTER TABLE payroll_batch_status_history ADD COLUMN IF NOT EXISTS actor_type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE payroll_batch_status_history ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(128);
CREATE INDEX IF NOT EXISTS idx_status_history_tenant_batch ON payroll_batch_status_history(tenant_id, batch_id, changed_at DESC);

ALTER TABLE webhook_endpoints ADD COLUMN IF NOT EXISTS tenant_id UUID;
UPDATE webhook_endpoints we
SET tenant_id = c.tenant_id
FROM companies c
WHERE we.company_id = c.id
  AND we.tenant_id IS NULL;
ALTER TABLE webhook_endpoints ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE webhook_endpoints ADD COLUMN IF NOT EXISTS secret_ciphertext VARCHAR(2000);
ALTER TABLE webhook_endpoints ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;
UPDATE webhook_endpoints SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE webhook_endpoints ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE webhook_endpoints
    ADD CONSTRAINT fk_webhook_endpoints_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

CREATE INDEX IF NOT EXISTS idx_payroll_batches_tenant_company_status
    ON payroll_batches(tenant_id, company_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_api_clients_tenant_company ON api_clients(tenant_id, company_id);
CREATE INDEX IF NOT EXISTS idx_webhook_endpoints_tenant_company ON webhook_endpoints(tenant_id, company_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_bank_accounts_company_hash
    ON bank_accounts(company_id, account_number_hash)
    WHERE account_number_hash IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_companies_tenant_tax_id
    ON companies(tenant_id, tax_id);
