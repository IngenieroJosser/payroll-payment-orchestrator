CREATE TABLE payroll_batches (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    source_account_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    scheduled_date DATE NOT NULL,
    status VARCHAR(40) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    total_payments INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE payroll_payments (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES payroll_batches(id) ON DELETE CASCADE,
    employee_document_type VARCHAR(20) NOT NULL,
    employee_document_number VARCHAR(50) NOT NULL,
    employee_full_name VARCHAR(180) NOT NULL,
    bank_code VARCHAR(20) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    account_number VARCHAR(60) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(40) NOT NULL
);

CREATE INDEX idx_payroll_batches_company_id ON payroll_batches(company_id);
CREATE INDEX idx_payroll_batches_status ON payroll_batches(status);
CREATE INDEX idx_payroll_payments_batch_id ON payroll_payments(batch_id);
CREATE INDEX idx_payroll_payments_status ON payroll_payments(status);
