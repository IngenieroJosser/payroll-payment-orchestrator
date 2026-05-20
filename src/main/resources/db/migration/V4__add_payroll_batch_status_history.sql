-- Tabla para registrar el historial de transiciones de estado de cada lote (Auditoría Financiera de Estados)
CREATE TABLE payroll_batch_status_history (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES payroll_batches(id) ON DELETE CASCADE,
    previous_status VARCHAR(40),
    new_status VARCHAR(40) NOT NULL,
    changed_by VARCHAR(120) NOT NULL,
    reason VARCHAR(250),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Índices de consulta rápida
CREATE INDEX idx_status_history_batch_id ON payroll_batch_status_history(batch_id);
CREATE INDEX idx_status_history_changed_at ON payroll_batch_status_history(changed_at);
