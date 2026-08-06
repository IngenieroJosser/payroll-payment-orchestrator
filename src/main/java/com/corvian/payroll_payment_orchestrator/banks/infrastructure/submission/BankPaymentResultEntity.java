package com.corvian.payroll_payment_orchestrator.banks.infrastructure.submission;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="bank_payment_results")
public class BankPaymentResultEntity {
    @Id private UUID id;
    @Column(name="submission_id",nullable=false) private UUID submissionId;
    @Column(name="payment_id",nullable=false) private UUID paymentId;
    @Column(name="external_payment_id",length=180) private String externalPaymentId;
    @Column(name="external_status",length=80) private String externalStatus;
    @Enumerated(EnumType.STRING) @Column(name="normalized_status",nullable=false,length=40) private PayrollPaymentStatus normalizedStatus;
    @Column(name="rejection_code",length=80) private String rejectionCode;
    @Column(name="rejection_reason",length=500) private String rejectionReason;
    @Column(name="settled_at") private OffsetDateTime settledAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getSubmissionId(){return submissionId;} public void setSubmissionId(UUID v){submissionId=v;}
    public UUID getPaymentId(){return paymentId;} public void setPaymentId(UUID v){paymentId=v;}
    public String getExternalPaymentId(){return externalPaymentId;} public void setExternalPaymentId(String v){externalPaymentId=v;}
    public String getExternalStatus(){return externalStatus;} public void setExternalStatus(String v){externalStatus=v;}
    public PayrollPaymentStatus getNormalizedStatus(){return normalizedStatus;} public void setNormalizedStatus(PayrollPaymentStatus v){normalizedStatus=v;}
    public String getRejectionCode(){return rejectionCode;} public void setRejectionCode(String v){rejectionCode=v;}
    public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;}
    public OffsetDateTime getSettledAt(){return settledAt;} public void setSettledAt(OffsetDateTime v){settledAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
}
