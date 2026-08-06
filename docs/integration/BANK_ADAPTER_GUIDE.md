# Guía para adaptadores bancarios

## Objetivo

Implementar un banco real sin contaminar controladores, dominio ni casos de uso con contratos propietarios.

## Contrato obligatorio

Cada adaptador implementa `BankPaymentProvider` y declara una `providerKey` única. Debe normalizar todos los estados externos a `BankSubmissionStatus` y `PayrollPaymentStatus`.

## Responsabilidades del adaptador

- Serialización al formato del banco: REST, SOAP, SFTP, archivo o ISO 20022.
- Autenticación, mTLS saliente, firma y validación de certificados según contrato.
- Timeouts y clasificación de errores retryable/non-retryable.
- Inclusión de la bank idempotency key cuando el banco la soporte.
- Validación del response y rechazo de mensajes incompletos.
- Mapeo de estados y códigos de rechazo.
- Redacción de secretos y PII en logs.
- Contract tests con fixtures autorizados.

## Responsabilidades que no pertenecen al adaptador

- Cambiar directamente el estado del lote.
- Persistir entidades JPA de nómina.
- Autorizar tenants o usuarios.
- Generar una nueva idempotency key en cada retry.
- Interpretar HTTP 200 como pago liquidado.

## Capabilities

`getCapabilities()` debe declarar de forma veraz:

- submission de lotes;
- consulta de lote;
- consulta individual;
- conciliación;
- idempotencia;
- máximo de pagos;
- monedas soportadas.

El coordinador valida estas capacidades antes de enviar.

## Contract test mínimo

- Submission aceptada.
- Rechazo funcional.
- Timeout y error 5xx retryable.
- Credencial inválida no retryable.
- Repetición con la misma idempotency key.
- Estado batch en procesamiento, parcial, liquidado y rechazado.
- Resultado individual rechazado/devuelto.
- Response incompleto o schema inválido.
- Conciliación idempotente.
- Ausencia de secretos en logs.


## Gestión de credenciales

En `staging` y `prod` está prohibido persistir tokens bancarios inline. La conexión debe usar `credentialReference` con formato `env:NOMBRE_VARIABLE`; la variable debe ser inyectada por Key Vault, Vault, un CSI driver o un mecanismo empresarial equivalente. El valor se resuelve solo al construir el perfil de ejecución y nunca se devuelve por la API.

Ejemplo:

```json
{
  "providerKey": "BANK_ACME",
  "credentialReference": "env:BANK_ACME_API_TOKEN"
}
```

El modo inline permanece únicamente para `dev/test` y compatibilidad local.

## Certificación

Un adaptador no debe habilitarse en producción hasta contar con:

1. Contrato y documentación del banco.
2. Certificados y secretos administrados por un vault.
3. Pruebas contractuales en sandbox oficial.
4. Pruebas de resiliencia y duplicados.
5. Evidencia de homologación del banco.
6. Runbook, alertas y propietario operativo.
