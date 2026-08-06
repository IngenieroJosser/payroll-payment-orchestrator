package com.corvian.payroll_payment_orchestrator.iam.infrastructure;

import com.corvian.payroll_payment_orchestrator.iam.domain.ApiClientStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_clients")
public class ApiClientEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "client_id", nullable = false, unique = true, length = 120)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 120)
    private String clientSecretHash;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 500)
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApiClientStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecretHash() { return clientSecretHash; }
    public void setClientSecretHash(String clientSecretHash) { this.clientSecretHash = clientSecretHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public ApiClientStatus getStatus() { return status; }
    public void setStatus(ApiClientStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
