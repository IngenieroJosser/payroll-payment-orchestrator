ALTER TABLE reconciliation_items ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE reconciliation_items ADD COLUMN IF NOT EXISTS company_id UUID;
ALTER TABLE reconciliation_items ADD COLUMN IF NOT EXISTS submission_id UUID;
ALTER TABLE reconciliation_items ADD COLUMN IF NOT EXISTS payment_id UUID;
ALTER TABLE reconciliation_items ADD COLUMN IF NOT EXISTS currency VARCHAR(3);
ALTER TABLE reconciliation_items ADD COLUMN IF NOT EXISTS difference_amount NUMERIC(19,2);
ALTER TABLE reconciliation_items ADD COLUMN IF NOT EXISTS source_event_id VARCHAR(180);
ALTER TABLE reconciliation_items ADD COLUMN IF NOT EXISTS details VARCHAR(500);

UPDATE reconciliation_items ri
SET tenant_id = pb.tenant_id,
    company_id = pb.company_id,
    currency = pb.currency,
    difference_amount = ri.bank_amount - ri.expected_amount
FROM payroll_batches pb
WHERE ri.batch_id = pb.id;

ALTER TABLE reconciliation_items ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE reconciliation_items ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE reconciliation_items ALTER COLUMN currency SET NOT NULL;
ALTER TABLE reconciliation_items ALTER COLUMN difference_amount SET NOT NULL;
ALTER TABLE reconciliation_items
    ADD CONSTRAINT fk_reconciliation_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE reconciliation_items
    ADD CONSTRAINT fk_reconciliation_company FOREIGN KEY (company_id) REFERENCES companies(id);
ALTER TABLE reconciliation_items
    ADD CONSTRAINT fk_reconciliation_submission FOREIGN KEY (submission_id) REFERENCES bank_submissions(id);
ALTER TABLE reconciliation_items
    ADD CONSTRAINT fk_reconciliation_payment FOREIGN KEY (payment_id) REFERENCES payroll_payments(id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_reconciliation_batch_reference
    ON reconciliation_items(batch_id, bank_reference);
CREATE UNIQUE INDEX IF NOT EXISTS uk_reconciliation_source_event
    ON reconciliation_items(source_event_id)
    WHERE source_event_id IS NOT NULL;
