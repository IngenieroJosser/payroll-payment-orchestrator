# Delivery manifest

Release: `1.1.0-enterprise-hardening`  
Date: `2026-08-06`  
Runtime target: `Java 21`  
Default enterprise mode: `RESTRICTED` (`APP_PAYMENT_EXECUTION_ENABLED=false`)

## Included

- 194 main Java source files.
- 17 test source files.
- 13 forward-only Flyway migrations.
- Production and staging profiles with mandatory external secrets.
- Controlled bank-provider certification allowlist and payment go-live gate.
- PostgreSQL/RabbitMQ reliability model, outbox/inbox and operational metrics.
- Kubernetes base plus Azure Key Vault/Workload Identity overlay.
- SLO, alerts, backup/DR, incident response, go-live and bank-certification procedures.
- CI security gates and a signed/provenance-attested container release workflow.

## Delivery-environment evidence

- Main sources compiled with `javac --release 21`: 212 class files.
- XML/YAML/JSON parsing passed.
- Flyway version uniqueness passed.
- Shell syntax checks passed.
- Core security/cryptography/deployment harnesses passed.
- Production preflight passed in restricted mode and rejected unsafe payment go-live input.

## Required release gate

The exact commit must pass `./mvnw -B -ntp clean verify` and the container release workflow in an environment with Maven Central and Docker access. The delivery sandbox could not download Maven 3.9.9, so this manifest does not claim that Maven/Testcontainers executed here.

## Financial boundary

This repository is deployable to companies in restricted mode. Real-money execution must remain disabled until a dedicated bank adapter has completed contractual homologation, security approval, failure-scenario testing and the documented go-live checklist. `SANDBOX` and `REST_GENERIC` are not production-certified providers.
