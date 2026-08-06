package com.corvian.payroll_payment_orchestrator.shared.messaging.inbox;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="inbox_messages")
public class InboxMessageEntity {
    @Id private UUID id;
    @Column(name="message_id",nullable=false,unique=true) private UUID messageId;
    @Column(name="message_type",nullable=false,length=120) private String messageType;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private InboxStatus status;
    @Column(name="attempt_count",nullable=false) private Integer attemptCount;
    @Column(name="correlation_id",length=128) private String correlationId;
    @Column(name="payload_hash",length=128) private String payloadHash;
    @Column(name="last_error",length=500) private String lastError;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="processed_at") private OffsetDateTime processedAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @Version private Long version;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getMessageId(){return messageId;} public void setMessageId(UUID v){messageId=v;}
    public String getMessageType(){return messageType;} public void setMessageType(String v){messageType=v;}
    public InboxStatus getStatus(){return status;} public void setStatus(InboxStatus v){status=v;}
    public Integer getAttemptCount(){return attemptCount;} public void setAttemptCount(Integer v){attemptCount=v;}
    public String getCorrelationId(){return correlationId;} public void setCorrelationId(String v){correlationId=v;}
    public String getPayloadHash(){return payloadHash;} public void setPayloadHash(String v){payloadHash=v;}
    public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getProcessedAt(){return processedAt;} public void setProcessedAt(OffsetDateTime v){processedAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}
