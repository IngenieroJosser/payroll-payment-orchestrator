-- Tabla mejorada de Idempotencia para respaldar bloqueos distribuidos y caché de respuestas a nivel DB
ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS response_body TEXT;
ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE;
