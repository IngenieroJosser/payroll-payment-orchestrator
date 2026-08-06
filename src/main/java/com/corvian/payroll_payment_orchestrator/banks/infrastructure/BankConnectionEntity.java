package com.corvian.payroll_payment_orchestrator.banks.infrastructure;

import com.corvian.payroll_payment_orchestrator.banks.domain.BankConnectionStatus;
import com.corvian.payroll_payment_orchestrator.shared.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_connections")
public class BankConnectionEntity {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "bank_code", nullable = false, length = 40) private String bankCode;
    @Column(name = "provider_key", nullable = false, length = 60) private String providerKey;
    @Column(name = "environment", nullable = false, length = 30) private String environment;
    @Column(name = "base_url", nullable = false, length = 500) private String baseUrl;
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_token_encrypted", length = 2000) private String apiToken;
    @Column(name = "credential_reference", length = 255) private String credentialReference;
    @Column(name = "credential_mode", nullable = false, length = 30) private String credentialMode;
    @Column(name = "connect_timeout_ms", nullable = false) private Integer connectTimeoutMs;
    @Column(name = "read_timeout_ms", nullable = false) private Integer readTimeoutMs;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30) private BankConnectionStatus status;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getProviderKey() { return providerKey; }
    public void setProviderKey(String providerKey) { this.providerKey = providerKey; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public String getCredentialReference() { return credentialReference; }
    public void setCredentialReference(String credentialReference) { this.credentialReference = credentialReference; }
    public String getCredentialMode() { return credentialMode; }
    public void setCredentialMode(String credentialMode) { this.credentialMode = credentialMode; }
    public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public Integer getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public BankConnectionStatus getStatus() { return status; }
    public void setStatus(BankConnectionStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
