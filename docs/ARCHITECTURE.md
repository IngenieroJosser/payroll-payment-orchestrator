# Arquitectura de producción

## 1. Estilo arquitectónico

El sistema es un **monolito modular** con una frontera hexagonal explícita alrededor del core financiero. Esta elección conserva transacciones fuertes, simplifica despliegue y auditoría, y evita introducir complejidad distribuida prematuramente. Los módulos pueden extraerse posteriormente porque los contratos de aplicación y los mensajes ya están versionados.

## 2. Módulos

| Módulo | Responsabilidad |
|---|---|
| `payroll` | Agregado de lote, pagos, estados y casos de uso |
| `banks` | Perfiles bancarios, resolución de adaptadores, submission y polling |
| `payments` | Consumo idempotente del comando de ejecución |
| `companies` | Empresas y cuentas de origen |
| `tenants` | Aislamiento organizacional |
| `iam` | Usuarios, clientes API, roles, permisos y tokens |
| `idempotency` | Replay seguro de comandos HTTP |
| `audit` | Evidencia operativa y financiera |
| `reconciliation` | Diferencias entre el ledger interno y evidencia bancaria |
| `webhooks` | Notificación firmada y reintentos |
| `shared.messaging` | Outbox, inbox y topología RabbitMQ |
| `observability` | Métricas financieras y técnicas |

## 3. Flujo de ejecución

```text
POST /payroll-batches/{id}/execute
  ├─ lock payroll batch
  ├─ validate tenant, company, maker-checker and state
  ├─ transition APPROVED → PROCESSING
  ├─ insert outbox event in the same PostgreSQL transaction
  └─ return 202

Outbox dispatcher
  ├─ SELECT ... FOR UPDATE SKIP LOCKED
  ├─ publish persistent RabbitMQ message
  ├─ wait for publisher confirm
  └─ mark PUBLISHED or RETRY/DEAD

Payroll consumer
  ├─ claim inbox message
  ├─ resolve company/source account/bank connection
  ├─ resolve BankPaymentProvider
  ├─ submit immutable payment snapshot with bank idempotency key
  ├─ persist external batch reference
  └─ transition PROCESSING → SENT_TO_BANK

Bank status worker
  ├─ claim due submissions with a database lease
  ├─ query provider status
  ├─ persist per-payment results
  ├─ normalize provider states
  └─ transition to PARTIALLY_PAID, PAID or FAILED
```

## 4. Consistencia e idempotencia

Se aplican cuatro barreras distintas:

1. **Idempotencia HTTP**: clave + endpoint + principal + tenant/company + hash del payload.
2. **Optimistic/pessimistic locking**: evita dobles transiciones del agregado.
3. **Outbox/inbox**: soporta entrega al menos una vez sin pérdida silenciosa.
4. **Bank idempotency key**: `PAYROLL-{batchId}`, estable ante redelivery.

La publicación a RabbitMQ no forma parte de la transacción PostgreSQL. La outbox elimina la ventana de pérdida; una duplicación posterior a un crash es absorbida por inbox, submission única e idempotencia bancaria.

## 5. Modelo de estados

La aceptación del proveedor y la liquidación son conceptos separados:

| Estado interno | Significado |
|---|---|
| `PROCESSING` | Ejecución iniciada internamente |
| `SENT_TO_BANK` | El proveedor aceptó técnicamente el lote |
| `PARTIALLY_PAID` | Existe evidencia de resultados mixtos |
| `PAID` | El proveedor confirmó liquidación completa |
| `FAILED` | Error terminal o agotamiento de polling |

Los pagos individuales soportan `PENDING`, `PROCESSING`, `SENT_TO_BANK`, `PAID`, `REJECTED`, `RETURNED`, `FAILED` y `CANCELLED`.

## 6. Multi-banco

La resolución se ejecuta en esta secuencia:

```text
tenant → company → source account → bank code
       → active bank connection → provider key → provider registry
```

El puerto estable es:

```java
public interface BankPaymentProvider {
    BankSubmissionResult submitPayrollBatch(BankSubmissionCommand command);
    BankPaymentStatusResult getBatchStatus(BankStatusQuery query);
    BankPaymentStatusResult getPaymentStatus(BankPaymentStatusQuery query);
    BankReconciliationResult reconcile(BankReconciliationCommand command);
    BankCapabilities getCapabilities();
}
```

Los adaptadores no controlan el dominio. Traducen el protocolo externo y devuelven resultados normalizados.

## 7. Datos sensibles

- Números de cuenta y documentos se cifran en reposo mediante AES-GCM.
- Hashes HMAC separados permiten búsquedas deterministas sin descifrar.
- REST y webhooks usan valores enmascarados o resúmenes sin PII.
- Tokens de conexión bancaria nunca se exponen en responses.
- Los payloads de outbox de ejecución contienen IDs, no instrucciones bancarias completas.

## 8. Reglas protegidas

ArchUnit impide que el dominio dependa de Spring, Jakarta Persistence, infraestructura o presentación. La deuda restante de capas administrativas tradicionales está aislada del agregado financiero y puede migrarse por módulo sin reescritura.

## 9. Decisiones y trade-offs

- **PostgreSQL sobre Redis para idempotencia**: consistencia transaccional y menor superficie operativa; Redis puede añadirse como optimización, no como fuente de verdad.
- **Polling más callbacks**: el puerto soporta ambos patrones; el polling garantiza recuperación cuando el banco no dispone de webhook confiable.
- **Monolito modular**: menor complejidad de consistencia para una fase sensible financieramente; los mensajes versionados permiten extracción posterior.
- **Adaptador REST genérico**: útil como referencia y para bancos con contratos equivalentes, pero no se presenta como certificación de ninguna entidad.
