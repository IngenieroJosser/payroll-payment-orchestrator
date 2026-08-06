package com.corvian.payroll_payment_orchestrator.audit.infrastructure;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;
    @Column(name = "resource_id")
    private UUID resourceId;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false, length = 120)
    private String actor;
    @Column(name = "actor_type", nullable = false, length = 30)
    private String actorType;
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "company_id")
    private UUID companyId;
    @Column(name = "correlation_id", length = 128)
    private String correlationId;
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    @Column(nullable = false, length = 30)
    private String result;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    @Column(name = "old_status", length = 40)
    private String oldStatus;
    @Column(name = "new_status", length = 40)
    private String newStatus;
    @Column(name = "trace_id", length = 64)
    private String traceId;
    @Column(name = "event_id")
    private UUID eventId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
}
