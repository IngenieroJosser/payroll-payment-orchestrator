# PRUEBA_ENDPOINT.md

Guia completa para probar en Postman todos los endpoints del backend `payroll-payment-orchestrator`.

## 1. Preparacion

Levantar dependencias y API local:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Base URL:

```text
http://localhost:8080
```

Credenciales admin locales:

```json
{
  "email": "admin@corvian.local",
  "password": "Admin123!"
}
```

## 2. Variables de Postman

Crea un Environment en Postman con estas variables:

```text
baseUrl = http://localhost:8080
token =
tenantId =
companyId =
bankAccountId =
batchId =
rejectBatchId =
webhookId =
clientId =
clientSecret =
```

En la Collection puedes configurar `Authorization` asi:

```text
Type: Bearer Token
Token: {{token}}
```

Para `GET /api/v1/health`, `POST /api/v1/auth/login`, `POST /api/v1/oauth/token`, Swagger y OpenAPI usa `No Auth`.

## 3. Headers importantes

Para endpoints protegidos:

```http
Authorization: Bearer {{token}}
Content-Type: application/json
```

Como `APP_REQUIRE_IDEMPOTENCY_KEY=true` por defecto, estos endpoints `POST` necesitan header `Idempotency-Key`:

```http
Idempotency-Key: {{$guid}}
```

No lo necesitan: `/auth/login`, `/oauth/token`, rutas `/iam/**` y rutas que contienen `/webhooks`.

## 4. Tests globales para Postman

Puedes pegar este script en `Tests` de casi todos los endpoints exitosos, cambiando el status esperado si aplica:

```javascript
pm.test("Respuesta JSON valida", function () {
  pm.response.to.be.json;
});

const json = pm.response.json();

pm.test("success true", function () {
  pm.expect(json.success).to.eql(true);
});
```

Para endpoints `GET` normalmente espera `200`. Para creaciones espera `201`. Para ejecutar nomina espera `202`.

## 5. Flujo recomendado

1. Probar health.
2. Login y guardar `token`.
3. Crear tenant y guardar `tenantId`.
4. Crear company y guardar `companyId`.
5. Crear bank account y guardar `bankAccountId`.
6. Crear API client si quieres probar OAuth client credentials.
7. Crear payroll batch y guardar `batchId`.
8. Validar, aprobar y ejecutar `batchId`.
9. Crear otro payroll batch para probar reject y guardar `rejectBatchId`.
10. Probar reconciliation, webhooks y audit logs.

## 6. Endpoints de aplicacion

### 1. GET `/api/v1/health`

Auth: `No Auth`

URL:

```text
{{baseUrl}}/api/v1/health
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Health UP", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.status).to.eql("UP");
});
```

### 2. POST `/api/v1/auth/login`

Auth: `No Auth`

URL:

```text
{{baseUrl}}/api/v1/auth/login
```

Body:

```json
{
  "email": "admin@corvian.local",
  "password": "Admin123!"
}
```

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Login exitoso", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.tokenType).to.eql("Bearer");
  pm.expect(json.data.accessToken).to.be.a("string").and.not.empty;
});

pm.environment.set("token", json.data.accessToken);
```

### 3. POST `/api/v1/oauth/token`

Auth: `No Auth`

Antes de probarlo, crea un API client con `POST /api/v1/iam/api-clients`.

URL:

```text
{{baseUrl}}/api/v1/oauth/token
```

Body:

```json
{
  "grantType": "client_credentials",
  "clientId": "{{clientId}}",
  "clientSecret": "{{clientSecret}}"
}
```

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Token de cliente generado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.accessToken).to.be.a("string").and.not.empty;
});
```

### 4. POST `/api/v1/tenants`

Auth: `Bearer {{token}}`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/tenants
```

Body:

```json
{
  "name": "Empresa Demo",
  "slug": "empresa-demo"
}
```

Status esperado: `201 Created`

Tests:

```javascript
pm.test("Status 201", function () {
  pm.response.to.have.status(201);
});

const json = pm.response.json();

pm.test("Tenant creado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.id).to.be.a("string").and.not.empty;
});

pm.environment.set("tenantId", json.data.id);
```

### 5. GET `/api/v1/tenants`

Auth: `Bearer {{token}}`

URL:

```text
{{baseUrl}}/api/v1/tenants
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Lista tenants", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

### 6. GET `/api/v1/tenants/{tenantId}`

Auth: `Bearer {{token}}`

URL:

```text
{{baseUrl}}/api/v1/tenants/{{tenantId}}
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Tenant encontrado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.id).to.eql(pm.environment.get("tenantId"));
});
```

### 7. POST `/api/v1/companies`

Auth: `Bearer {{token}}`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/companies
```

Body:

```json
{
  "tenantId": "{{tenantId}}",
  "legalName": "Empresa Demo S.A.S.",
  "taxId": "900123456-7",
  "currency": "COP"
}
```

Status esperado: `201 Created`

Tests:

```javascript
pm.test("Status 201", function () {
  pm.response.to.have.status(201);
});

const json = pm.response.json();

pm.test("Company creada", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.id).to.be.a("string").and.not.empty;
});

pm.environment.set("companyId", json.data.id);
```

### 8. GET `/api/v1/companies`

Auth: `Bearer {{token}}`

URL:

```text
{{baseUrl}}/api/v1/companies
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Lista companies", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

### 9. GET `/api/v1/companies/{companyId}`

Auth: `Bearer {{token}}`

URL:

```text
{{baseUrl}}/api/v1/companies/{{companyId}}
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Company encontrada", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.id).to.eql(pm.environment.get("companyId"));
});
```

### 10. POST `/api/v1/companies/{companyId}/bank-accounts`

Auth: `Bearer {{token}}`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/companies/{{companyId}}/bank-accounts
```

Body:

```json
{
  "bankCode": "001",
  "accountType": "SAVINGS",
  "accountNumber": "1234567890"
}
```

Valores validos para `accountType`: `SAVINGS`, `CHECKING`.

Status esperado: `201 Created`

Tests:

```javascript
pm.test("Status 201", function () {
  pm.response.to.have.status(201);
});

const json = pm.response.json();

pm.test("Cuenta bancaria creada", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.id).to.be.a("string").and.not.empty;
});

pm.environment.set("bankAccountId", json.data.id);
```

### 11. GET `/api/v1/companies/{companyId}/bank-accounts`

Auth: `Bearer {{token}}`

URL:

```text
{{baseUrl}}/api/v1/companies/{{companyId}}/bank-accounts
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Lista bank accounts", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

### 12. POST `/api/v1/iam/users`

Auth: `Bearer {{token}}`

Permiso requerido: `iam:manage`

No requiere `Idempotency-Key`.

URL:

```text
{{baseUrl}}/api/v1/iam/users
```

Body:

```json
{
  "tenantId": "{{tenantId}}",
  "companyId": "{{companyId}}",
  "email": "operador@corvian.local",
  "fullName": "Operador Nomina",
  "password": "Operador123!",
  "roles": ["PAYROLL_OPERATOR"]
}
```

Roles validos: `ADMIN`, `AUDITOR`, `PAYROLL_OPERATOR`, `APPROVER`, `BANK_OPERATOR`.

Status esperado: `201 Created`

Tests:

```javascript
pm.test("Status 201", function () {
  pm.response.to.have.status(201);
});

const json = pm.response.json();

pm.test("Usuario creado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.email).to.eql("operador@corvian.local");
});
```

Nota: si repites el mismo email debe responder `400 Bad Request` con `USER_ALREADY_EXISTS`.

### 13. GET `/api/v1/iam/users`

Auth: `Bearer {{token}}`

Permiso requerido: `iam:read`

URL:

```text
{{baseUrl}}/api/v1/iam/users
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Lista users", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

### 14. POST `/api/v1/iam/api-clients`

Auth: `Bearer {{token}}`

Permiso requerido: `iam:manage`

No requiere `Idempotency-Key`.

URL:

```text
{{baseUrl}}/api/v1/iam/api-clients
```

Body:

```json
{
  "companyId": "{{companyId}}",
  "name": "Cliente integracion nomina",
  "scopes": ["payroll:create", "payroll:read", "payroll:execute"]
}
```

Status esperado: `201 Created`

Tests:

```javascript
pm.test("Status 201", function () {
  pm.response.to.have.status(201);
});

const json = pm.response.json();

pm.test("API client creado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.clientId).to.be.a("string").and.not.empty;
  pm.expect(json.data.clientSecret).to.be.a("string").and.not.empty;
});

pm.environment.set("clientId", json.data.clientId);
pm.environment.set("clientSecret", json.data.clientSecret);
```

Nota: `clientSecret` solo se devuelve al crearlo.

### 15. POST `/api/v1/bank-connections`

Auth: `Bearer {{token}}`

Permiso requerido: `bank:manage`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/bank-connections
```

Body:

```json
{
  "companyId": "{{companyId}}",
  "bankCode": "001",
  "baseUrl": "https://sandbox.bank.local",
  "apiToken": "sandbox-token"
}
```

Status esperado: `201 Created`

Tests:

```javascript
pm.test("Status 201", function () {
  pm.response.to.have.status(201);
});

const json = pm.response.json();

pm.test("Bank connection creada", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.status).to.eql("ACTIVE");
});
```

### 16. POST `/api/v1/payroll-batches`

Auth: `Bearer {{token}}`

Permiso requerido: `payroll:create`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/payroll-batches
```

Body:

```json
{
  "companyId": "{{companyId}}",
  "sourceAccountId": "{{bankAccountId}}",
  "currency": "COP",
  "scheduledDate": "2026-06-01",
  "payments": [
    {
      "employeeDocumentType": "CC",
      "employeeDocumentNumber": "123456789",
      "employeeFullName": "Juan Perez",
      "bankCode": "001",
      "accountType": "SAVINGS",
      "accountNumber": "1234567890",
      "amount": 2500000
    }
  ]
}
```

Status esperado: `201 Created`

Tests:

```javascript
pm.test("Status 201", function () {
  pm.response.to.have.status(201);
});

const json = pm.response.json();

pm.test("Payroll batch creado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.status).to.eql("DRAFT");
  pm.expect(json.data.totalPayments).to.eql(1);
});

pm.environment.set("batchId", json.data.id);
```

Para probar rechazo, duplica esta misma request, ejecutala de nuevo y cambia la ultima linea del test por:

```javascript
pm.environment.set("rejectBatchId", json.data.id);
```

Luego ejecuta `validate` usando `{{rejectBatchId}}` antes de llamar el endpoint de reject.

### 17. GET `/api/v1/payroll-batches`

Auth: `Bearer {{token}}`

Permiso requerido: `payroll:read`

URL:

```text
{{baseUrl}}/api/v1/payroll-batches
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Lista payroll batches", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

### 18. GET `/api/v1/payroll-batches/{batchId}`

Auth: `Bearer {{token}}`

Permiso requerido: `payroll:read`

URL:

```text
{{baseUrl}}/api/v1/payroll-batches/{{batchId}}
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Payroll batch encontrado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.id).to.eql(pm.environment.get("batchId"));
});
```

### 19. POST `/api/v1/payroll-batches/{batchId}/validate`

Auth: `Bearer {{token}}`

Permiso requerido: `payroll:create`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/payroll-batches/{{batchId}}/validate
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Payroll batch validado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.status).to.eql("PENDING_APPROVAL");
});
```

### 20. POST `/api/v1/payroll-batches/{batchId}/approve`

Auth: `Bearer {{token}}`

Permiso requerido: `payroll:approve`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/payroll-batches/{{batchId}}/approve
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Payroll batch aprobado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.status).to.eql("APPROVED");
});
```

### 21. POST `/api/v1/payroll-batches/{batchId}/execute`

Auth: `Bearer {{token}}`

Permiso requerido: `payroll:execute`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/payroll-batches/{{batchId}}/execute
```

Body: ninguno.

Status esperado: `202 Accepted`

Tests:

```javascript
pm.test("Status 202", function () {
  pm.response.to.have.status(202);
});

const json = pm.response.json();

pm.test("Payroll batch enviado a ejecucion", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.status).to.eql("PROCESSING");
});
```

### 22. POST `/api/v1/payroll-batches/{batchId}/reject`

Auth: `Bearer {{token}}`

Permiso requerido: `payroll:approve`

Importante: crea otro batch y validalo antes de probar reject. El batch debe estar en `PENDING_APPROVAL`.

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/payroll-batches/{{rejectBatchId}}/reject
```

Body:

```json
{
  "reason": "Datos bancarios incompletos"
}
```

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Payroll batch rechazado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.status).to.eql("REJECTED");
});
```

### 23. POST `/api/v1/payroll-batches/{batchId}/reconciliation`

Auth: `Bearer {{token}}`

Permiso requerido: `reconciliation:manage`

Headers:

```http
Idempotency-Key: {{$guid}}
```

URL:

```text
{{baseUrl}}/api/v1/payroll-batches/{{batchId}}/reconciliation
```

Body:

```json
{
  "bankReference": "BANK-REF-001",
  "bankAmount": 2500000
}
```

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Conciliacion creada", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(["MATCHED", "MISMATCHED"]).to.include(json.data.status);
});
```

### 24. GET `/api/v1/payroll-batches/{batchId}/reconciliation`

Auth: `Bearer {{token}}`

Permiso requerido: `payroll:read`

URL:

```text
{{baseUrl}}/api/v1/payroll-batches/{{batchId}}/reconciliation
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Lista conciliaciones", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

### 25. POST `/api/v1/companies/{companyId}/webhooks`

Auth: `Bearer {{token}}`

No requiere `Idempotency-Key`.

URL:

```text
{{baseUrl}}/api/v1/companies/{{companyId}}/webhooks
```

Body:

```json
{
  "url": "https://webhook.site/tu-url-de-prueba"
}
```

Status esperado: `201 Created`

Tests:

```javascript
pm.test("Status 201", function () {
  pm.response.to.have.status(201);
});

const json = pm.response.json();

pm.test("Webhook creado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.enabled).to.eql(true);
});

pm.environment.set("webhookId", json.data.id);
```

### 26. GET `/api/v1/companies/{companyId}/webhooks`

Auth: `Bearer {{token}}`

URL:

```text
{{baseUrl}}/api/v1/companies/{{companyId}}/webhooks
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Lista webhooks", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

### 27. POST `/api/v1/companies/{companyId}/webhooks/{webhookId}/disable`

Auth: `Bearer {{token}}`

No requiere `Idempotency-Key`.

URL:

```text
{{baseUrl}}/api/v1/companies/{{companyId}}/webhooks/{{webhookId}}/disable
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Webhook deshabilitado", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data.status).to.eql("DISABLED");
});
```

### 28. GET `/api/v1/audit-logs`

Auth: `Bearer {{token}}`

Permiso requerido: `audit:read`

URL:

```text
{{baseUrl}}/api/v1/audit-logs
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Lista audit logs", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

### 29. GET `/api/v1/audit-logs?resourceId={resourceId}`

Auth: `Bearer {{token}}`

Permiso requerido: `audit:read`

URL:

```text
{{baseUrl}}/api/v1/audit-logs?resourceId={{batchId}}
```

Body: ninguno.

Status esperado: `200 OK`

Tests:

```javascript
pm.test("Status 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("Audit logs filtrados", function () {
  pm.expect(json.success).to.eql(true);
  pm.expect(json.data).to.be.an("array");
});
```

## 7. Rutas tecnicas

### 30. GET `/actuator/health`

Auth: `No Auth`

URL:

```text
{{baseUrl}}/actuator/health
```

Status esperado: `200 OK`

### 31. GET `/actuator/info`

Auth: `No Auth`

URL:

```text
{{baseUrl}}/actuator/info
```

Status esperado: `200 OK`

### 32. GET `/actuator/metrics`

Auth: `Bearer {{token}}`

URL:

```text
{{baseUrl}}/actuator/metrics
```

Status esperado: `200 OK`

### 33. GET `/v3/api-docs`

Auth: `No Auth`

URL:

```text
{{baseUrl}}/v3/api-docs
```

Status esperado: `200 OK`

### 34. GET `/swagger-ui/index.html`

Auth: `No Auth`

URL:

```text
{{baseUrl}}/swagger-ui/index.html
```

Status esperado: `200 OK`

### 35. GET `/swagger-ui.html`

Auth: `No Auth`

URL:

```text
{{baseUrl}}/swagger-ui.html
```

Status esperado: `200 OK` o redireccion a Swagger UI.

## 8. Pruebas negativas recomendadas

### Sin token en endpoint protegido

Ejemplo:

```text
GET {{baseUrl}}/api/v1/tenants
```

Sin header `Authorization`.

Resultado esperado: `401 Unauthorized` o `403 Forbidden`, segun filtro de seguridad.

### Sin Idempotency-Key

Ejemplo:

```text
POST {{baseUrl}}/api/v1/tenants
```

Con token, pero sin header `Idempotency-Key`.

Resultado esperado: `400 Bad Request`.

Error esperado:

```json
{
  "success": false,
  "error": {
    "code": "IDEMPOTENCY_KEY_REQUIRED"
  }
}
```

### Body invalido

Ejemplo:

```json
{
  "name": "",
  "slug": "slug invalido con espacios"
}
```

Resultado esperado: `400 Bad Request`, `error.code` igual a `VALIDATION_ERROR`.

### Estado invalido de nomina

Ejemplo: ejecutar un batch que todavia esta en `DRAFT`.

Resultado esperado: `400 Bad Request`, error de estado invalido.

## 9. Checklist completa

```text
[ ] GET  /api/v1/health
[ ] POST /api/v1/auth/login
[ ] POST /api/v1/oauth/token
[ ] POST /api/v1/tenants
[ ] GET  /api/v1/tenants
[ ] GET  /api/v1/tenants/{tenantId}
[ ] POST /api/v1/companies
[ ] GET  /api/v1/companies
[ ] GET  /api/v1/companies/{companyId}
[ ] POST /api/v1/companies/{companyId}/bank-accounts
[ ] GET  /api/v1/companies/{companyId}/bank-accounts
[ ] POST /api/v1/iam/users
[ ] GET  /api/v1/iam/users
[ ] POST /api/v1/iam/api-clients
[ ] POST /api/v1/bank-connections
[ ] POST /api/v1/payroll-batches
[ ] GET  /api/v1/payroll-batches
[ ] GET  /api/v1/payroll-batches/{batchId}
[ ] POST /api/v1/payroll-batches/{batchId}/validate
[ ] POST /api/v1/payroll-batches/{batchId}/approve
[ ] POST /api/v1/payroll-batches/{batchId}/execute
[ ] POST /api/v1/payroll-batches/{batchId}/reject
[ ] POST /api/v1/payroll-batches/{batchId}/reconciliation
[ ] GET  /api/v1/payroll-batches/{batchId}/reconciliation
[ ] POST /api/v1/companies/{companyId}/webhooks
[ ] GET  /api/v1/companies/{companyId}/webhooks
[ ] POST /api/v1/companies/{companyId}/webhooks/{webhookId}/disable
[ ] GET  /api/v1/audit-logs
[ ] GET  /api/v1/audit-logs?resourceId={resourceId}
[ ] GET  /actuator/health
[ ] GET  /actuator/info
[ ] GET  /actuator/metrics
[ ] GET  /v3/api-docs
[ ] GET  /swagger-ui/index.html
[ ] GET  /swagger-ui.html
```
