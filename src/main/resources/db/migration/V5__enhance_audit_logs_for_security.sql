-- Enriquecimiento de la tabla de auditoría con columnas de IP y trazabilidad
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS actor VARCHAR(120) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(128);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS client_ip VARCHAR(45);

-- Índice por trazabilidad
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation ON audit_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor);
