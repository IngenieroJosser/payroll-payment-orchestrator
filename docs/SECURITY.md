# Modelo de seguridad

## Controles implementados

### Identidad y autorización

- Sesiones stateless.
- Tokens HMAC con `iss`, `aud`, `iat`, `nbf`, `exp`, `jti`, actor type, tenant y company.
- Roles y permisos por scope.
- Autorización por método con `@PreAuthorize`.
- Validación adicional de propiedad del recurso; los UUID no constituyen autorización.
- Clientes API y usuarios aislados por tenant/company.

### Secretos y criptografía

- Inicio fail-fast si JWT, encryption key o HMAC key tienen menos de 32 bytes.
- Rechazo de marcadores inseguros fuera de perfiles `dev`/`test`.
- AES-GCM con nonce aleatorio y prefijo de versión para rotación futura.
- HMAC-SHA256 para claves de búsqueda e integridad de inbox/webhooks.
- Bootstrap administrativo deshabilitado por defecto y siempre deshabilitado en `prod`.

### Transporte y perímetro

- mTLS opcional para clientes entrantes, con allowlist de subjects.
- IP allowlist compatible con proxies explícitamente confiables.
- Rate limit sin bloqueo artificial de threads.
- URLs salientes limitadas a HTTPS, hosts permitidos y redes no privadas en producción.
- Redirects HTTP deshabilitados en conectores y webhooks.

### Privacidad

- Documento y cuenta bancaria se enmascaran en responses.
- Los webhooks no transportan beneficiarios, documentos ni cuentas.
- Los errores no devuelven stack traces.
- Los logs sanitizan saltos de línea y no registran tokens ni payloads financieros completos.

## Configuración obligatoria de producción

- Mantenga secretos en Azure Key Vault, HashiCorp Vault, Kubernetes Secrets cifrados o un servicio equivalente.
- Termine TLS 1.2+ en un ingress confiable y preserve el certificado cliente cuando mTLS esté activado.
- Defina `APP_TRUSTED_PROXY_ADDRESSES`; no confíe globalmente en `X-Forwarded-For`.
- Defina `APP_OUTBOUND_ALLOWED_HOSTS` con FQDN exactos.
- Use una cuenta PostgreSQL de mínimo privilegio y otra identidad separada para migraciones cuando la plataforma lo permita.
- Restrinja `/actuator/prometheus` a la red de observabilidad.
- Deshabilite Swagger en producción; el perfil `prod` ya lo hace.

## Rotación de claves

La versión actual cifra con prefijo `v2:` y permite lectura dual mediante `APP_PREVIOUS_ENCRYPTION_KEY`. Para una rotación:

1. Mover la clave activa actual a `APP_PREVIOUS_ENCRYPTION_KEY`.
2. Generar una nueva `APP_ENCRYPTION_KEY` independiente y desplegar lectura dual.
3. Re-cifrar filas en lotes auditados; toda escritura nueva usa la clave activa.
4. Verificar conteos, descifrado y rollback.
5. Retirar la clave anterior solo cuando ya no existan ciphertexts que dependan de ella.

No rote una clave eliminando la anterior antes de completar la migración de datos.

## Riesgos que requieren controles externos

- Un adaptador bancario real debe aportar mTLS saliente, firma de mensajes, truststore, pinning o HSM según el contrato de la entidad.
- La prevención de fraude requiere políticas empresariales adicionales: límites, listas de beneficiarios, MFA/step-up para aprobación y segregación de funciones configurable.
- WAF, DDoS protection, SIEM, backups, gestión de parches y respuesta a incidentes pertenecen a la plataforma de despliegue.

## Reporte de vulnerabilidades

No publique credenciales, datos de nómina ni evidencia bancaria en issues públicos. Use el canal privado definido por la organización operadora.
