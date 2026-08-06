# Informe de implementación productiva

> **Historical foundation report.** The current enterprise deployment posture, V13 changes, restricted/payment modes and final validation are documented in [`ENTERPRISE_READINESS_REPORT.md`](ENTERPRISE_READINESS_REPORT.md).

## Resumen

Se evolucionó el repositorio existente sin reescritura completa ni eliminación de endpoints. El flujo que marcaba un lote como pagado inmediatamente después del sandbox fue reemplazado por submission persistente, aceptación técnica, consulta de estado y liquidación confirmada.

## Cambios principales

### Dominio

- Invariantes de moneda, montos, cuentas y conteos.
- Máquina de estados explícita.
- Resultados por pago y preservación de pagos ya liquidados ante fallos parciales.
- Maker-checker básico y metadata de aprobación.

### Seguridad

- Tenant/company en la identidad autenticada.
- Autorización por propiedad de recurso.
- JWT endurecido.
- PII enmascarada.
- Secretos fail-fast y bootstrap deshabilitado por defecto.
- SSRF, proxy trust, mTLS entrante y rate limit configurables.

### Integración bancaria

- Puerto multi-adaptador requerido.
- Resolver dinámico por perfil bancario.
- Sandbox determinista y REST genérico de referencia.
- External batch id, estado, intentos, polling y resultados individuales persistidos.
- Circuit breaker y métricas por provider/banco.

### Mensajería

- Transactional outbox.
- Publisher confirms.
- Inbox con estados procesado/en curso/recuperable.
- Retry queue y DLQ.
- Mensajes versionados y correlation IDs.
- Claim distribuido mediante `SKIP LOCKED`.

### Auditoría y webhooks

- Historial de transición escrito realmente.
- Auditoría con actor, tenant, company, resultado y trazas.
- Webhooks firmados, secreto one-time, payload estable y retry seguro.

### Operación

- Migraciones V7–V12.
- Docker multi-stage y usuario no-root.
- Prometheus, OpenTelemetry y health probes.
- GitHub Actions con `clean verify`, Testcontainers, JaCoCo, SBOM y build de imagen.

## Validaciones realizadas en este entorno

- Compilación limpia con `javac --release 21 -parameters` de todo `src/main/java`: 202 clases generadas.
- `git diff --check` sin errores.
- Arnés ejecutable aprobado para máquina de estados, normalización financiera, AES-GCM/HMAC, emisión y validación JWT, contrato sandbox y política de URLs salientes.
- Parseo de YAML/XML y validaciones estáticas de secretos, PII, estados y mensajería.

## Validación no ejecutada localmente

`./mvnw clean verify` no pudo ejecutarse porque este entorno bloqueó la descarga de la distribución Maven. El workflow de GitHub Actions contiene el gate completo y debe ejecutarse en una red con acceso a Maven Central antes de promover el artefacto.

## Endurecimiento final

- Lease recuperable para claves de idempotencia bloqueadas por una caída de proceso.
- Lease recuperable para entregas webhook detenidas en `SENDING`.
- Requisito obligatorio de idempotencia y consulta de estado en cualquier adaptador bancario utilizado para nómina.
- Health probes excluidos de filtros de perímetro para permitir liveness/readiness controlados.
- `INVALID_CLIENT` se devuelve como `401 Unauthorized`.

## Prerrequisitos antes de procesar dinero real

- Ejecutar `./mvnw -B -ntp clean verify` en CI con acceso a Maven Central y Docker.
- Implementar y homologar el adaptador específico de cada banco.
- Integrar secretos y claves con KMS/HSM o Azure Key Vault y definir rotación operativa.
- Configurar mTLS/firma saliente, egress allowlist y controles de red del banco.
- Ejecutar pruebas de penetración, carga, recuperación, backup/restore y continuidad operativa.
- Aprobar monitoreo, alertas, runbooks, RTO/RPO y segregación de funciones con el área financiera.

## Límites deliberados

- No se inventaron endpoints ni contratos de Bancolombia, Davivienda, BBVA u otra entidad.
- `REST_GENERIC` es una referencia, no una homologación bancaria.
- mTLS saliente, firma, HSM y formato de cada banco pertenecen a su adaptador real.
- El sistema necesita pruebas de homologación con cada entidad y controles de plataforma antes de manejar dinero real.
