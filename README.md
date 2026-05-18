# Payroll Payment Orchestrator

Backend Spring Boot para orquestación de pagos de nómina empresarial mediante API REST.

## Stack

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- RabbitMQ
- Spring Security
- OpenAPI / Swagger
- Docker Compose

## Ejecutar entorno local

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

En Linux/macOS:

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Servicios

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- RabbitMQ Management: http://localhost:15672

## Credenciales locales

PostgreSQL:

- Host: localhost
- Puerto: 5432
- Base de datos: payroll-payment-orchestrator
- Usuario: payroll_user
- Password: payroll_password

RabbitMQ:

- Usuario: payroll_user
- Password: payroll_password

## Flujo de prueba

1. Crear tenant: `POST /api/v1/tenants`
2. Crear empresa: `POST /api/v1/companies`
3. Crear cuenta bancaria: `POST /api/v1/companies/{companyId}/bank-accounts`
4. Crear lote de nómina: `POST /api/v1/payroll-batches`
5. Validar lote: `POST /api/v1/payroll-batches/{batchId}/validate`
6. Aprobar lote: `POST /api/v1/payroll-batches/{batchId}/approve`
7. Ejecutar lote: `POST /api/v1/payroll-batches/{batchId}/execute`
8. Consultar auditoría: `GET /api/v1/audit-logs?resourceId={batchId}`

## Endpoints principales

- `GET /api/v1/health`
- `POST /api/v1/tenants`
- `GET /api/v1/tenants`
- `POST /api/v1/companies`
- `GET /api/v1/companies`
- `POST /api/v1/companies/{companyId}/bank-accounts`
- `POST /api/v1/payroll-batches`
- `GET /api/v1/payroll-batches`
- `GET /api/v1/payroll-batches/{batchId}`
- `POST /api/v1/payroll-batches/{batchId}/validate`
- `POST /api/v1/payroll-batches/{batchId}/approve`
- `POST /api/v1/payroll-batches/{batchId}/reject`
- `POST /api/v1/payroll-batches/{batchId}/execute`
- `GET /api/v1/audit-logs`
