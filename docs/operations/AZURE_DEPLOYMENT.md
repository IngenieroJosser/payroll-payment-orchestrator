# Azure production deployment reference

A typical enterprise topology uses:

- Azure Front Door or Application Gateway with WAF for controlled ingress.
- AKS across availability zones for the application runtime.
- Azure Database for PostgreSQL Flexible Server with private networking, HA, PITR and customer-approved retention.
- RabbitMQ operated by an approved managed provider or Kubernetes operator with quorum queues and tested recovery.
- Azure Key Vault with Workload Identity and the Secrets Store CSI Driver.
- Azure Monitor/Application Insights or an OpenTelemetry collector feeding the enterprise observability platform.
- Private DNS, Private Link where supported, Azure Firewall/NAT and explicit egress allowlisting for bank hosts.
- ACR with immutable tags/digests, image scanning and release signing.

## Separation of duties

Use distinct identities for runtime, Flyway migration, backup, CI/CD and human administration. The runtime identity must not create databases, modify roles or read unrelated Key Vault secrets.

## Go-live sequence

1. Deploy with `APP_PAYMENT_EXECUTION_ENABLED=false`.
2. Validate migrations, health groups, alerts, audit export and restore procedure.
3. Complete bank certification and load production trust material from Key Vault.
4. Add only dedicated certified provider keys to `APP_BANK_CERTIFIED_PROVIDERS`.
5. Enable payment execution in a separately approved change with enhanced monitoring.
