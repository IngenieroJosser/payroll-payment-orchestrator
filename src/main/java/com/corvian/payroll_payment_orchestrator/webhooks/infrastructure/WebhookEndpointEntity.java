package com.corvian.payroll_payment_orchestrator.webhooks.infrastructure;

import com.corvian.payroll_payment_orchestrator.shared.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="webhook_endpoints")
public class WebhookEndpointEntity {
    @Id private UUID id;
    @Column(name="tenant_id",nullable=false) private UUID tenantId;
    @Column(name="company_id",nullable=false) private UUID companyId;
    @Column(nullable=false,length=500) private String url;
    @Column(nullable=false,length=200) private String secret;
    @Convert(converter=EncryptedStringConverter.class)
    @Column(name="secret_ciphertext",length=2000) private String secretCiphertext;
    @Column(nullable=false) private Boolean enabled;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getTenantId(){return tenantId;} public void setTenantId(UUID v){tenantId=v;}
    public UUID getCompanyId(){return companyId;} public void setCompanyId(UUID v){companyId=v;}
    public String getUrl(){return url;} public void setUrl(String v){url=v;}
    public String getSecret(){return secret;} public void setSecret(String v){secret=v;}
    public String getSecretCiphertext(){return secretCiphertext;} public void setSecretCiphertext(String v){secretCiphertext=v;}
    public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean v){enabled=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
}
