package com.corvian.payroll_payment_orchestrator.reconciliation.infrastructure;

import com.corvian.payroll_payment_orchestrator.reconciliation.domain.ReconciliationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="reconciliation_items")
public class ReconciliationItemEntity {
    @Id private UUID id;
    @Column(name="tenant_id",nullable=false) private UUID tenantId;
    @Column(name="company_id",nullable=false) private UUID companyId;
    @Column(name="batch_id",nullable=false) private UUID batchId;
    @Column(name="submission_id") private UUID submissionId;
    @Column(name="payment_id") private UUID paymentId;
    @Column(name="bank_reference",nullable=false,length=120) private String bankReference;
    @Column(name="source_event_id",length=180) private String sourceEventId;
    @Column(nullable=false,length=3) private String currency;
    @Column(name="expected_amount",nullable=false,precision=19,scale=2) private BigDecimal expectedAmount;
    @Column(name="bank_amount",nullable=false,precision=19,scale=2) private BigDecimal bankAmount;
    @Column(name="difference_amount",nullable=false,precision=19,scale=2) private BigDecimal differenceAmount;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ReconciliationStatus status;
    @Column(length=500) private String details;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getTenantId(){return tenantId;} public void setTenantId(UUID v){tenantId=v;}
    public UUID getCompanyId(){return companyId;} public void setCompanyId(UUID v){companyId=v;}
    public UUID getBatchId(){return batchId;} public void setBatchId(UUID v){batchId=v;}
    public UUID getSubmissionId(){return submissionId;} public void setSubmissionId(UUID v){submissionId=v;}
    public UUID getPaymentId(){return paymentId;} public void setPaymentId(UUID v){paymentId=v;}
    public String getBankReference(){return bankReference;} public void setBankReference(String v){bankReference=v;}
    public String getSourceEventId(){return sourceEventId;} public void setSourceEventId(String v){sourceEventId=v;}
    public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
    public BigDecimal getExpectedAmount(){return expectedAmount;} public void setExpectedAmount(BigDecimal v){expectedAmount=v;}
    public BigDecimal getBankAmount(){return bankAmount;} public void setBankAmount(BigDecimal v){bankAmount=v;}
    public BigDecimal getDifferenceAmount(){return differenceAmount;} public void setDifferenceAmount(BigDecimal v){differenceAmount=v;}
    public ReconciliationStatus getStatus(){return status;} public void setStatus(ReconciliationStatus v){status=v;}
    public String getDetails(){return details;} public void setDetails(String v){details=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
}
