package com.corvian.payroll_payment_orchestrator.reconciliation.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.usecase.PayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.reconciliation.domain.ReconciliationStatus;
import com.corvian.payroll_payment_orchestrator.reconciliation.infrastructure.JpaReconciliationItemRepository;
import com.corvian.payroll_payment_orchestrator.reconciliation.infrastructure.ReconciliationItemEntity;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {
    private final JpaReconciliationItemRepository repository;
    private final PayrollBatchUseCase payrollBatchUseCase;
    private final PayrollBatchRepositoryPort batchRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ReconciliationService(JpaReconciliationItemRepository repository, PayrollBatchUseCase payrollBatchUseCase,
                                 PayrollBatchRepositoryPort batchRepository, AuditLogService auditLogService, Clock clock) {
        this.repository = repository; this.payrollBatchUseCase = payrollBatchUseCase;
        this.batchRepository = batchRepository; this.auditLogService = auditLogService; this.clock = clock;
    }

    @Transactional
    public ReconciliationItemEntity reconcile(UUID batchId, String bankReference, BigDecimal bankAmount) {
        return reconcile(batchId, bankReference, bankAmount, null, null);
    }

    @Transactional
    public ReconciliationItemEntity reconcile(UUID batchId, String bankReference, BigDecimal bankAmount,
                                               String sourceEventId, String details) {
        var batch = payrollBatchUseCase.findById(batchId);
        var metadata = batchRepository.findMetadata(batchId).orElseThrow();
        String reference = normalize(bankReference, 120, "bank reference");
        if (bankAmount == null || bankAmount.signum() < 0) {
            throw new DomainException("INVALID_RECONCILIATION_AMOUNT", "Bank amount cannot be negative");
        }
        BigDecimal normalizedAmount;
        try { normalizedAmount = bankAmount.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new DomainException("INVALID_RECONCILIATION_AMOUNT", "Bank amount supports at most two decimal places"); }

        if (sourceEventId != null && !sourceEventId.isBlank()) {
            var existing = repository.findBySourceEventId(sourceEventId.trim());
            if (existing.isPresent()) return existing.get();
        }
        var duplicate = repository.findByBatchIdAndBankReference(batchId, reference);
        if (duplicate.isPresent()) {
            ReconciliationItemEntity existing = duplicate.get();
            if (existing.getBankAmount().compareTo(normalizedAmount) != 0) {
                throw new DomainException("RECONCILIATION_REFERENCE_REUSED", "Bank reference was already reconciled with a different amount");
            }
            return existing;
        }

        BigDecimal difference = normalizedAmount.subtract(batch.totalAmount()).setScale(2);
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setId(UUID.randomUUID()); item.setTenantId(metadata.tenantId()); item.setCompanyId(batch.companyId());
        item.setBatchId(batchId); item.setBankReference(reference); item.setSourceEventId(blankToNull(sourceEventId));
        item.setCurrency(batch.currency()); item.setExpectedAmount(batch.totalAmount()); item.setBankAmount(normalizedAmount);
        item.setDifferenceAmount(difference);
        item.setStatus(difference.signum() == 0 ? ReconciliationStatus.MATCHED : ReconciliationStatus.MISMATCHED);
        item.setDetails(details == null ? null : normalize(details, 500, "details"));
        item.setCreatedAt(OffsetDateTime.now(clock));
        ReconciliationItemEntity saved = repository.save(item);
        auditLogService.record("PAYROLL_BATCH_RECONCILED", "PAYROLL_BATCH", batchId,
                "Reconciliation status: " + saved.getStatus(), metadata.tenantId(), batch.companyId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationItemEntity> findByBatch(UUID batchId) {
        payrollBatchUseCase.findById(batchId);
        return repository.findByBatchIdOrderByCreatedAtDesc(batchId);
    }

    private String normalize(String value, int max, String field) {
        if (value == null || value.isBlank()) throw new DomainException("INVALID_RECONCILIATION_REFERENCE", field + " is required");
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        if (normalized.length() > max) throw new DomainException("INVALID_RECONCILIATION_REFERENCE", field + " is too long");
        return normalized;
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
