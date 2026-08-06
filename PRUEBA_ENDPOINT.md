# Guía de prueba de API

La colección [`PRUEBAS-POSTMAN.json`](PRUEBAS-POSTMAN.json) no contiene contraseñas, tokens ni secretos embebidos. Configure sus variables antes de ejecutar.

## Requisitos

1. Infraestructura PostgreSQL y RabbitMQ disponible.
2. Aplicación iniciada con `dev` para pruebas locales o `prod` con controles externos habilitados.
3. Dos identidades distintas para maker-checker: creador y aprobador.
4. Tenant, empresa, cuenta de origen y perfil bancario creados previamente.

## Variables mínimas

- `baseUrl`
- `creatorEmail`, `creatorPassword`
- `approverEmail`, `approverPassword`
- `companyId`, `sourceAccountId`
- Datos de beneficiario de prueba

La colección obtiene los tokens y captura `batchId` automáticamente. Los requests mutables utilizan una clave `Idempotency-Key` nueva por ejecución.

## Flujo

```text
Login creador → Login aprobador → Crear lote → Validar → Aprobar → Ejecutar → Consultar
```

No reutilice datos bancarios reales en ambientes de desarrollo. Para instrucciones HTTP equivalentes consulte [`docs/api/payroll-sample-requests.http`](docs/api/payroll-sample-requests.http).
