package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="idempotency_keys")
public class IdempotencyKeyEntity {
    @Id private UUID id;
    @Column(name="idempotency_key",nullable=false,length=120) private String idempotencyKey;
    @Column(nullable=false,length=200) private String endpoint;
    @Column(name="request_hash",length=128) private String requestHash;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="expires_at",nullable=false) private OffsetDateTime expiresAt;
    @Column(name="response_body",columnDefinition="TEXT") private String responseBody;
    @Column(name="response_status") private Integer responseStatus;
    @Column(name="response_content_type",length=120) private String responseContentType;
    @Column(nullable=false) private boolean locked;
    @Column(name="locked_at") private OffsetDateTime lockedAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
    public String getEndpoint(){return endpoint;} public void setEndpoint(String v){endpoint=v;}
    public String getRequestHash(){return requestHash;} public void setRequestHash(String v){requestHash=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v; if(updatedAt==null) updatedAt=v;}
    public OffsetDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(OffsetDateTime v){expiresAt=v;}
    public String getResponseBody(){return responseBody;} public void setResponseBody(String v){responseBody=v;}
    public Integer getResponseStatus(){return responseStatus;} public void setResponseStatus(Integer v){responseStatus=v;}
    public String getResponseContentType(){return responseContentType;} public void setResponseContentType(String v){responseContentType=v;}
    public boolean isLocked(){return locked;} public void setLocked(boolean v){locked=v;}
    public OffsetDateTime getLockedAt(){return lockedAt;} public void setLockedAt(OffsetDateTime v){lockedAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
}
