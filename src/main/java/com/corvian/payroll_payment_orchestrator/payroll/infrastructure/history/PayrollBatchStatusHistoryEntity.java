package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.history;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_batch_status_history")
public class PayrollBatchStatusHistoryEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "company_id")
    private UUID companyId;
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 40)
    private PayrollBatchStatus previousStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 40)
    private PayrollBatchStatus newStatus;
    @Column(name = "changed_by", nullable = false, length = 120)
    private String changedBy;
    @Column(name = "actor_type", nullable = false, length = 30)
    private String actorType;
    @Column(length = 250)
    private String reason;
    @Column(name = "correlation_id", length = 128)
    private String correlationId;
    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public void setPreviousStatus(PayrollBatchStatus previousStatus) { this.previousStatus = previousStatus; }
    public void setNewStatus(PayrollBatchStatus newStatus) { this.newStatus = newStatus; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }
}
