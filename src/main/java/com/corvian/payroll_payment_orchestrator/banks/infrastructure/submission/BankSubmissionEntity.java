package com.corvian.payroll_payment_orchestrator.banks.infrastructure.submission;

import com.corvian.payroll_payment_orchestrator.banks.application.model.BankSubmissionStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="bank_submissions")
public class BankSubmissionEntity {
    @Id private UUID id;
    @Column(name="tenant_id",nullable=false) private UUID tenantId;
    @Column(name="company_id",nullable=false) private UUID companyId;
    @Column(name="batch_id",nullable=false) private UUID batchId;
    @Column(name="bank_connection_id",nullable=false) private UUID bankConnectionId;
    @Column(name="execution_id",nullable=false,unique=true) private UUID executionId;
    @Column(name="provider_key",nullable=false,length=60) private String providerKey;
    @Column(name="bank_idempotency_key",nullable=false,length=180) private String bankIdempotencyKey;
    @Column(name="external_batch_id",length=180) private String externalBatchId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private BankSubmissionStatus status;
    @Column(name="attempt_count",nullable=false) private Integer attemptCount;
    @Column(name="last_error_code",length=80) private String lastErrorCode;
    @Column(name="last_error_message",length=500) private String lastErrorMessage;
    @Column(name="submitted_at") private OffsetDateTime submittedAt;
    @Column(name="next_status_poll_at") private OffsetDateTime nextStatusPollAt;
    @Column(name="last_status_check_at") private OffsetDateTime lastStatusCheckAt;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @Version private Long version;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public UUID getTenantId(){return tenantId;} public void setTenantId(UUID v){tenantId=v;}
    public UUID getCompanyId(){return companyId;} public void setCompanyId(UUID v){companyId=v;}
    public UUID getBatchId(){return batchId;} public void setBatchId(UUID v){batchId=v;}
    public UUID getBankConnectionId(){return bankConnectionId;} public void setBankConnectionId(UUID v){bankConnectionId=v;}
    public UUID getExecutionId(){return executionId;} public void setExecutionId(UUID v){executionId=v;}
    public String getProviderKey(){return providerKey;} public void setProviderKey(String v){providerKey=v;}
    public String getBankIdempotencyKey(){return bankIdempotencyKey;} public void setBankIdempotencyKey(String v){bankIdempotencyKey=v;}
    public String getExternalBatchId(){return externalBatchId;} public void setExternalBatchId(String v){externalBatchId=v;}
    public BankSubmissionStatus getStatus(){return status;} public void setStatus(BankSubmissionStatus v){status=v;}
    public Integer getAttemptCount(){return attemptCount;} public void setAttemptCount(Integer v){attemptCount=v;}
    public String getLastErrorCode(){return lastErrorCode;} public void setLastErrorCode(String v){lastErrorCode=v;}
    public String getLastErrorMessage(){return lastErrorMessage;} public void setLastErrorMessage(String v){lastErrorMessage=v;}
    public OffsetDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(OffsetDateTime v){submittedAt=v;}
    public OffsetDateTime getNextStatusPollAt(){return nextStatusPollAt;} public void setNextStatusPollAt(OffsetDateTime v){nextStatusPollAt=v;}
    public OffsetDateTime getLastStatusCheckAt(){return lastStatusCheckAt;} public void setLastStatusCheckAt(OffsetDateTime v){lastStatusCheckAt=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
}
