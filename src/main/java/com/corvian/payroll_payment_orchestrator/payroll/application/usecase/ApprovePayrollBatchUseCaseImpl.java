package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchMetadata;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ApprovePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.config.PayrollProperties;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.PayrollBatchNotFoundException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorContext;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ResourceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ApprovePayrollBatchUseCaseImpl implements ApprovePayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final AuditLogService auditLogService;
    private final ResourceAccessService accessService;
    private final ActorContext actorContext;
    private final PayrollBatchStatusHistoryService historyService;
    private final PayrollProperties properties;
    private final Clock clock;

    public ApprovePayrollBatchUseCaseImpl(
            PayrollBatchRepositoryPort repository,
            AuditLogService auditLogService,
            ResourceAccessService accessService,
            ActorContext actorContext,
            PayrollBatchStatusHistoryService historyService,
            PayrollProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.accessService = accessService;
        this.actorContext = actorContext;
        this.historyService = historyService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PayrollBatch approve(UUID batchId) {
        PayrollBatch batch = locked(batchId);
        PayrollBatchMetadata metadata = metadata(batchId);
        requireAccess(metadata);
        String actor = actorContext.actorName();
        if (properties.isMakerCheckerRequired() && metadata.createdBy() != null && metadata.createdBy().equalsIgnoreCase(actor)) {
            throw new DomainException("MAKER_CHECKER_VIOLATION", "Payroll creator cannot approve the same payroll batch");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        PayrollBatch updated = repository.save(batch.transitionTo(PayrollBatchStatus.APPROVED, now));
        repository.recordApproval(batchId, actor, now);
        recordTransition(batchId, metadata, batch.status(), updated.status(), "Payroll batch approved", now);
        auditLogService.record("PAYROLL_BATCH_APPROVED", "PAYROLL_BATCH", updated.id(), "Payroll batch approved",
                metadata.tenantId(), metadata.companyId());
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch reject(UUID batchId, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() < 5) {
            throw new DomainException("REJECTION_REASON_REQUIRED", "A meaningful rejection reason is required");
        }
        PayrollBatch batch = locked(batchId);
        PayrollBatchMetadata metadata = metadata(batchId);
        requireAccess(metadata);
        OffsetDateTime now = OffsetDateTime.now(clock);
        String safeReason = sanitize(reason, 500);
        PayrollBatch updated = repository.save(batch.transitionTo(PayrollBatchStatus.REJECTED, now));
        repository.recordRejection(batchId, safeReason);
        recordTransition(batchId, metadata, batch.status(), updated.status(), safeReason, now);
        auditLogService.record("PAYROLL_BATCH_REJECTED", "PAYROLL_BATCH", updated.id(),
                "Payroll batch rejected: " + safeReason, metadata.tenantId(), metadata.companyId());
        return updated;
    }

    private PayrollBatch locked(UUID batchId) {
        return repository.findByIdForUpdate(batchId).orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
    }

    private PayrollBatchMetadata metadata(UUID batchId) {
        return repository.findMetadata(batchId).orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
    }

    private void requireAccess(PayrollBatchMetadata metadata) {
        accessService.requireCompanyAccess(metadata.companyId());
    }

    private void recordTransition(UUID batchId, PayrollBatchMetadata metadata, PayrollBatchStatus from, PayrollBatchStatus to, String reason, OffsetDateTime at) {
        historyService.record(metadata.tenantId(), metadata.companyId(), batchId, from, to, reason, at);
    }

    private String sanitize(String value, int maxLength) {
        String sanitized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
