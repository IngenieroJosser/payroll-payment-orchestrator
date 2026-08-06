package com.corvian.payroll_payment_orchestrator.shared.messaging.outbox;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id private UUID id;
    @Column(name="aggregate_type", nullable=false, length=80) private String aggregateType;
    @Column(name="aggregate_id", nullable=false) private UUID aggregateId;
    @Column(name="event_type", nullable=false, length=120) private String eventType;
    @Column(name="event_version", nullable=false) private Integer eventVersion;
    @Column(name="routing_key", nullable=false, length=160) private String routingKey;
    @Column(nullable=false, columnDefinition="TEXT") private String payload;
    @Column(name="correlation_id", length=128) private String correlationId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private OutboxStatus status;
    @Column(name="attempt_count", nullable=false) private Integer attemptCount;
    @Column(name="next_attempt_at", nullable=false) private OffsetDateTime nextAttemptAt;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    @Column(name="published_at") private OffsetDateTime publishedAt;
    @Column(name="last_error", length=500) private String lastError;
    @Version private Long version;

    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public String getAggregateType(){return aggregateType;} public void setAggregateType(String v){aggregateType=v;}
    public UUID getAggregateId(){return aggregateId;} public void setAggregateId(UUID v){aggregateId=v;}
    public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    public Integer getEventVersion(){return eventVersion;} public void setEventVersion(Integer v){eventVersion=v;}
    public String getRoutingKey(){return routingKey;} public void setRoutingKey(String v){routingKey=v;}
    public String getPayload(){return payload;} public void setPayload(String v){payload=v;}
    public String getCorrelationId(){return correlationId;} public void setCorrelationId(String v){correlationId=v;}
    public OutboxStatus getStatus(){return status;} public void setStatus(OutboxStatus v){status=v;}
    public Integer getAttemptCount(){return attemptCount;} public void setAttemptCount(Integer v){attemptCount=v;}
    public OffsetDateTime getNextAttemptAt(){return nextAttemptAt;} public void setNextAttemptAt(OffsetDateTime v){nextAttemptAt=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getPublishedAt(){return publishedAt;} public void setPublishedAt(OffsetDateTime v){publishedAt=v;}
    public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}
