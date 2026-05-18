
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(250) NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(250) NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    company_id UUID,
    email VARCHAR(180) NOT NULL UNIQUE,
    full_name VARCHAR(180) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS api_clients (
    id UUID PRIMARY KEY,
    company_id UUID,
    client_id VARCHAR(120) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(120) NOT NULL,
    name VARCHAR(160) NOT NULL,
    scopes VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS account_number_encrypted VARCHAR(2000);
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS account_number_hash VARCHAR(128);
ALTER TABLE payroll_payments ALTER COLUMN employee_document_number TYPE VARCHAR(2000);
ALTER TABLE payroll_payments ALTER COLUMN account_number TYPE VARCHAR(2000);
ALTER TABLE payroll_payments ADD COLUMN IF NOT EXISTS employee_document_hash VARCHAR(128);
ALTER TABLE payroll_payments ADD COLUMN IF NOT EXISTS account_number_hash VARCHAR(128);
ALTER TABLE payroll_payments ADD COLUMN IF NOT EXISTS account_number_last4 VARCHAR(4);

CREATE TABLE IF NOT EXISTS bank_connections (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    bank_code VARCHAR(40) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    api_token_encrypted VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS webhook_delivery_attempts (
    id UUID PRIMARY KEY,
    webhook_endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id),
    event VARCHAR(120) NOT NULL,
    resource_id UUID,
    attempt INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    http_status INTEGER,
    error_message VARCHAR(500),
    next_retry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS reconciliation_items (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES payroll_batches(id),
    bank_reference VARCHAR(120) NOT NULL,
    expected_amount NUMERIC(19,2) NOT NULL,
    bank_amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_api_clients_client_id ON api_clients(client_id);
CREATE INDEX IF NOT EXISTS idx_bank_connections_company ON bank_connections(company_id);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_retry ON webhook_delivery_attempts(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_reconciliation_batch ON reconciliation_items(batch_id);
