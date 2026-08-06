package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payroll_batches")
public class PayrollBatchEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PayrollBatchStatus status;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "total_payments", nullable = false)
    private Integer totalPayments;
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PayrollPaymentEntity> payments = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Column(name = "created_by", length = 160)
    private String createdBy;
    @Column(name = "approved_by", length = 160)
    private String approvedBy;
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    @Version
    @Column(nullable = false)
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(UUID sourceAccountId) { this.sourceAccountId = sourceAccountId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public PayrollBatchStatus getStatus() { return status; }
    public void setStatus(PayrollBatchStatus status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getTotalPayments() { return totalPayments; }
    public void setTotalPayments(Integer totalPayments) { this.totalPayments = totalPayments; }
    public List<PayrollPaymentEntity> getPayments() { return payments; }
    public void setPayments(List<PayrollPaymentEntity> payments) { this.payments = payments; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
