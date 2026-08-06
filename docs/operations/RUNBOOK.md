# Runbook operativo

## 1. Indicadores principales

Supervise como mínimo:

- `payroll.bank.submissions` por provider/status.
- `payroll.bank.submission.duration` y `payroll.bank.status.duration`.
- Profundidad de `payroll.execution.queue`, retry queue y DLQ.
- Filas `outbox_events` en `RETRY` o `DEAD`.
- Filas `inbox_messages` en `PROCESSING` por más de seis minutos.
- `bank_submissions` con polling vencido o intentos cercanos al máximo.
- Webhooks en `FAILED` y latencia de entrega.
- Pool Hikari, conexiones RabbitMQ, tasa 4xx/5xx y latencia HTTP.

## 2. Outbox atascada

```sql
SELECT status, count(*)
FROM outbox_events
GROUP BY status;

SELECT id, event_type, attempt_count, next_attempt_at, last_error
FROM outbox_events
WHERE status IN ('RETRY', 'DEAD')
ORDER BY created_at;
```

Acciones:

1. Verifique RabbitMQ, exchange, binding y credenciales.
2. Corrija la causa antes de reactivar eventos.
3. Para un evento `DEAD`, documente el incidente y cambie a `RETRY` con `next_attempt_at = now()` solo después de confirmar que no generará un pago duplicado.
4. La bank idempotency key y el inbox protegen el reintento, pero debe conservarse el mismo payload.

## 3. Mensajes en DLQ

No vuelva a publicar ciegamente. Inspeccione headers `messageId`, `eventType`, `eventVersion`, `correlationId` y `x-death`.

- Payload malformado: corrija productor/versión; no reprocesar sin transformación controlada.
- Timeout bancario: verifique la submission persistida y su external batch id antes de reintentar.
- Error definitivo del proveedor: mantenga evidencia y escale al operador bancario.

## 4. Submission sin avance

```sql
SELECT id, batch_id, provider_key, external_batch_id, status,
       attempt_count, next_status_poll_at, last_error_code, last_error_message
FROM bank_submissions
WHERE status IN ('ACCEPTED', 'PROCESSING', 'PARTIALLY_SETTLED')
ORDER BY next_status_poll_at;
```

Verifique primero el estado directamente con el canal autorizado del banco. Nunca cambie un lote a `PAID` por intervención manual sin evidencia bancaria y aprobación auditada.

## 5. Conciliación

- Compare por payment reference, external payment id, monto, moneda y fecha valor.
- Un total agregado coincidente no demuestra conciliación individual.
- Registre cada fuente mediante `source_event_id` para evitar reingesta.
- Mantenga archivos originales en almacenamiento inmutable con retención definida externamente.

## 6. Backups y recuperación

- PostgreSQL: PITR, backups cifrados y pruebas periódicas de restauración.
- RabbitMQ: las colas no reemplazan el ledger PostgreSQL; recupere outbox/inbox y luego restablezca mensajería.
- RPO/RTO deben definirse contractualmente por organización.
- Después de una restauración, pause workers bancarios, valide submissions externas y solo entonces reanude polling/outbox.

## 7. Despliegue

1. Ejecutar `./mvnw clean verify`.
2. Respaldar y ejecutar Flyway en staging con una copia representativa.
3. Validar conectividad PostgreSQL, RabbitMQ, hosts bancarios y observabilidad.
4. Desplegar una instancia con workers controlados.
5. Verificar health/readiness, migraciones y métricas.
6. Escalar horizontalmente.
7. Ejecutar un pago de certificación no monetario o sandbox conforme al proceso del banco.

## 8. Rollback

No revierta migraciones Flyway destructivamente. Para rollback de aplicación:

- Confirme compatibilidad backward de schema.
- Detenga nuevos comandos de ejecución.
- Mantenga polling de submissions ya aceptadas o transfiera su responsabilidad a una versión compatible.
- No borre outbox, inbox ni submissions.

## 9. Emergency payment stop

Set `APP_PAYMENT_EXECUTION_ENABLED=false` and roll out the configuration change. This blocks new execution commands while preserving bank status polling, reconciliation evidence, outbox/inbox and audit records. Follow `INCIDENT_RESPONSE.md` before resuming.

## 10. Alert catalogue

Prometheus-compatible examples are available in `ops/prometheus/payroll-alerts.yml`. Alert routing, paging windows and client escalation contacts must be configured outside the application repository.
