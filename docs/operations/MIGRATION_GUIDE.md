# Guía de migración V7–V13

## Principio

Las migraciones V1–V6 se conservan sin modificación. V7–V13 añaden controles de producción y deben probarse sobre una copia de staging antes de ejecutarse en producción.

## Preflight obligatorio

Ejecute y resuelva cualquier fila devuelta:

```sql
-- Lotes con empresa inexistente
SELECT pb.id, pb.company_id
FROM payroll_batches pb
LEFT JOIN companies c ON c.id = pb.company_id
WHERE c.id IS NULL;

-- Lotes con cuenta de origen inexistente o de otra empresa
SELECT pb.id, pb.company_id, pb.source_account_id
FROM payroll_batches pb
LEFT JOIN bank_accounts ba ON ba.id = pb.source_account_id
WHERE ba.id IS NULL OR ba.company_id <> pb.company_id;

-- Conexiones, webhooks y clientes API con company inválida
SELECT 'bank_connection' AS type, bc.id
FROM bank_connections bc LEFT JOIN companies c ON c.id = bc.company_id WHERE c.id IS NULL
UNION ALL
SELECT 'webhook', we.id
FROM webhook_endpoints we LEFT JOIN companies c ON c.id = we.company_id WHERE c.id IS NULL
UNION ALL
SELECT 'api_client', ac.id
FROM api_clients ac LEFT JOIN companies c ON c.id = ac.company_id
WHERE ac.company_id IS NOT NULL AND c.id IS NULL;

-- Duplicados que violarían índices únicos
SELECT company_id, account_number_hash, count(*)
FROM bank_accounts
WHERE account_number_hash IS NOT NULL
GROUP BY company_id, account_number_hash
HAVING count(*) > 1;
```

No elimine datos para hacer pasar una migración. Corrija cada inconsistencia con evidencia y auditoría.

## Cambios por versión

### V7

- Tenant explícito en lotes, conexiones, webhooks y clientes API.
- FKs de nómina hacia tenant, empresa y cuenta.
- `@Version` para lotes.
- Maker-checker metadata.
- Contexto ampliado de auditoría e historial.

### V8

- Submission bancaria y resultados por pago.
- Transactional outbox e inbox.

### V9

- Payload estable, hash, versionado y timestamps de webhooks.

### V10

- Tenant/company/currency/difference y deduplicación en conciliación.

### V11

- Código HTTP y content type en replay idempotente.

### V12

- Lease temporal para recuperar claves de idempotencia que quedaron bloqueadas tras una caída del proceso.
- Índice de recuperación para entregas webhook detenidas en estado `SENDING`.
- Los workers pueden reclamar trabajo estancado una vez vencido el lease configurable, sin duplicar entregas activas.

## Estrategia de despliegue

1. Pausar escrituras de nómina durante el cambio de V6 a V7 si existen datos históricos inconsistentes.
2. Tomar backup verificable.
3. Ejecutar Flyway.
4. Comprobar `flyway_schema_history` y `hibernate.ddl-auto=validate`.
5. Ejecutar smoke tests de lectura, creación, aprobación y sandbox.
6. Reanudar workers.

## Verificación posterior

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT count(*) FROM payroll_batches WHERE tenant_id IS NULL;
SELECT count(*) FROM bank_connections WHERE tenant_id IS NULL;
SELECT count(*) FROM webhook_endpoints WHERE tenant_id IS NULL;
```

Todos los conteos de tenant nulo deben ser cero en tablas operativas que lo exigen.

## V13 — Referencias externas de credenciales e índices operativos

- Añade `credential_reference` y `credential_mode` a las conexiones bancarias.
- Conserva temporalmente credenciales cifradas existentes para una migración controlada.
- Añade una restricción que mantiene coherencia entre el modo y la referencia externa.
- Añade índices para recuperación de outbox, inbox, submissions bancarias y entregas webhook.
- Antes de activar pagos en staging o producción, cada conexión no sandbox debe migrarse a una referencia `env:<VARIABLE>` respaldada por el gestor de secretos aprobado.
