# Correction report v2

## Scope

Localized corrections only. No financial domain behavior, REST contract, migration, security policy, bank orchestration, outbox/inbox, or tenant isolation logic was removed.

## Corrected

1. `PayrollBatch` now contains one total calculator named `calculateTotal`; no duplicate `sumPayments` declaration remains.
2. `PostgresContainerSmokeTest` now contains one `DynamicPropertySource` method and one smoke test method.
3. PostgreSQL and RabbitMQ both use Testcontainers `GenericContainer`, removing the dependency on `JdbcDatabaseContainer` and the database-specific Testcontainers module.
4. The POM keeps only `testcontainers` and `testcontainers-junit-jupiter` under the Testcontainers 2.0.5 BOM.
5. Java remains pinned to release 21.

## Local extraction rule

Extract this package into a new empty directory. Do not merge it over a directory containing manually edited or duplicated Java files.
