package com.corvian.payroll_payment_orchestrator.webhooks.infrastructure;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="webhook_delivery_attempts")
public class WebhookDeliveryAttemptEntity {
    @Id private UUID id;
    @Column(name="webhook_endpoint_id",nullable=false) private UUID webhookEndpointId;
    @Column(name="event_id") private UUID eventId;
    @Column(nullable=false,length=120) private String event;
    @Column(name="resource_id") private UUID resourceId;
    @Column(nullable=false) private Integer attempt;
    @Column(nullable=false,length=30) private String status;
    @Column(name="http_status") private Integer httpStatus;
    @Column(name="error_message",length=500) private String errorMessage;
    @Column(name="next_retry_at") private OffsetDateTime nextRetryAt;
    @Column(columnDefinition="TEXT") private String payload;
    @Column(name="payload_hash",length=128) private String payloadHash;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @Version private Long version;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getWebhookEndpointId(){return webhookEndpointId;} public void setWebhookEndpointId(UUID v){webhookEndpointId=v;}
    public UUID getEventId(){return eventId;} public void setEventId(UUID v){eventId=v;}
    public String getEvent(){return event;} public void setEvent(String v){event=v;}
    public UUID getResourceId(){return resourceId;} public void setResourceId(UUID v){resourceId=v;}
    public Integer getAttempt(){return attempt;} public void setAttempt(Integer v){attempt=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Integer getHttpStatus(){return httpStatus;} public void setHttpStatus(Integer v){httpStatus=v;}
    public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
    public OffsetDateTime getNextRetryAt(){return nextRetryAt;} public void setNextRetryAt(OffsetDateTime v){nextRetryAt=v;}
    public String getPayload(){return payload;} public void setPayload(String v){payload=v;}
    public String getPayloadHash(){return payloadHash;} public void setPayloadHash(String v){payloadHash=v;}
    public OffsetDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(OffsetDateTime v){completedAt=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}
