package com.corvian.payroll_payment_orchestrator.banks.application;

import com.corvian.payroll_payment_orchestrator.banks.application.model.BankPaymentResult;
import com.corvian.payroll_payment_orchestrator.banks.application.model.BankSubmissionResult;
import com.corvian.payroll_payment_orchestrator.banks.application.model.BankSubmissionStatus;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.submission.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BankSubmissionStore {
    private final JpaBankSubmissionRepository repository;
    private final JpaBankPaymentResultRepository paymentResultRepository;
    private final Clock clock;

    public BankSubmissionStore(JpaBankSubmissionRepository repository,
                               JpaBankPaymentResultRepository paymentResultRepository, Clock clock) {
        this.repository = repository;
        this.paymentResultRepository = paymentResultRepository;
        this.clock = clock;
    }

    @Transactional
    public BankSubmissionEntity prepare(UUID tenantId, UUID companyId, UUID batchId, UUID connectionId,
                                        UUID executionId, String providerKey, String idempotencyKey) {
        Optional<BankSubmissionEntity> current = repository.findFirstByBatchIdOrderByCreatedAtDesc(batchId);
        if (current.isPresent()) {
            BankSubmissionEntity existing = current.get();
            if (existing.getStatus() != BankSubmissionStatus.FAILED && existing.getStatus() != BankSubmissionStatus.REJECTED) {
                return existing;
            }
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        BankSubmissionEntity entity = current.orElseGet(BankSubmissionEntity::new);
        if (entity.getId() == null) entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setCompanyId(companyId);
        entity.setBatchId(batchId);
        entity.setBankConnectionId(connectionId);
        entity.setExecutionId(executionId);
        entity.setProviderKey(providerKey);
        entity.setBankIdempotencyKey(idempotencyKey);
        entity.setStatus(BankSubmissionStatus.PREPARED);
        entity.setAttemptCount(entity.getAttemptCount() == null ? 0 : entity.getAttemptCount());
        entity.setLastErrorCode(null);
        entity.setLastErrorMessage(null);
        if (entity.getCreatedAt() == null) entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return repository.saveAndFlush(entity);
    }

    @Transactional
    public BankSubmissionEntity markSubmitting(UUID id) {
        BankSubmissionEntity entity = repository.findById(id).orElseThrow();
        entity.setStatus(BankSubmissionStatus.SUBMITTING);
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setUpdatedAt(OffsetDateTime.now(clock));
        return entity;
    }

    @Transactional
    public BankSubmissionEntity recordSubmissionResult(UUID id, BankSubmissionResult result, Duration pollDelay) {
        BankSubmissionEntity entity = repository.findById(id).orElseThrow();
        OffsetDateTime now = OffsetDateTime.now(clock);
        entity.setExternalBatchId(result.externalBatchId());
        entity.setStatus(result.status());
        entity.setSubmittedAt(result.receivedAt() == null ? now : result.receivedAt());
        entity.setUpdatedAt(now);
        entity.setLastErrorCode(null);
        entity.setLastErrorMessage(null);
        entity.setNextStatusPollAt(result.status().terminal() ? null : now.plus(pollDelay));
        return entity;
    }

    @Transactional
    public void recordRetryableFailure(UUID id, String code, String message) {
        BankSubmissionEntity entity = repository.findById(id).orElseThrow();
        entity.setStatus(BankSubmissionStatus.PREPARED);
        entity.setLastErrorCode(limit(code, 80));
        entity.setLastErrorMessage(limit(message, 500));
        entity.setUpdatedAt(OffsetDateTime.now(clock));
    }

    @Transactional
    public void recordTerminalFailure(UUID id, String code, String message) {
        BankSubmissionEntity entity = repository.findById(id).orElseThrow();
        entity.setStatus(BankSubmissionStatus.FAILED);
        entity.setLastErrorCode(limit(code, 80));
        entity.setLastErrorMessage(limit(message, 500));
        entity.setNextStatusPollAt(null);
        entity.setUpdatedAt(OffsetDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public BankSubmissionEntity get(UUID id) { return repository.findById(id).orElseThrow(); }

    @Transactional
    public List<UUID> claimDueSubmissionIds(OffsetDateTime now, int limit, Duration lease) {
        List<BankSubmissionEntity> due = repository.lockDueForPolling(now, limit);
        OffsetDateTime leasedUntil = now.plus(lease);
        for (BankSubmissionEntity entity : due) {
            entity.setNextStatusPollAt(leasedUntil);
            entity.setUpdatedAt(now);
        }
        List<UUID> claimedIds = new java.util.ArrayList<>(due.size());
        for (BankSubmissionEntity entity : due) {
            claimedIds.add(entity.getId());
        }
        return List.copyOf(claimedIds);
    }

    @Transactional
    public BankSubmissionEntity recordStatus(UUID id, BankSubmissionStatus status, List<BankPaymentResult> results,
                                             Duration nextDelay) {
        BankSubmissionEntity entity = repository.findById(id).orElseThrow();
        OffsetDateTime now = OffsetDateTime.now(clock);
        entity.setStatus(status);
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setLastStatusCheckAt(now);
        entity.setUpdatedAt(now);
        entity.setNextStatusPollAt(status.terminal() ? null : now.plus(nextDelay));
        for (BankPaymentResult result : results) {
            BankPaymentResultEntity payment = paymentResultRepository.findBySubmissionIdAndPaymentId(id, result.paymentId())
                    .orElseGet(BankPaymentResultEntity::new);
            if (payment.getId() == null) payment.setId(UUID.randomUUID());
            payment.setSubmissionId(id);
            payment.setPaymentId(result.paymentId());
            payment.setExternalPaymentId(result.externalPaymentId());
            payment.setExternalStatus(limit(result.externalStatus(), 80));
            payment.setNormalizedStatus(result.status());
            payment.setRejectionCode(limit(result.rejectionCode(), 80));
            payment.setRejectionReason(limit(result.rejectionReason(), 500));
            payment.setSettledAt(result.settledAt());
            payment.setUpdatedAt(now);
            paymentResultRepository.save(payment);
        }
        return entity;
    }


    @Transactional
    public void recordPollFailure(UUID id, String code, String message, Duration retryDelay) {
        BankSubmissionEntity entity = repository.findById(id).orElseThrow();
        OffsetDateTime now = OffsetDateTime.now(clock);
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setLastErrorCode(limit(code, 80));
        entity.setLastErrorMessage(limit(message, 500));
        entity.setLastStatusCheckAt(now);
        entity.setNextStatusPollAt(now.plus(retryDelay));
        entity.setUpdatedAt(now);
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
