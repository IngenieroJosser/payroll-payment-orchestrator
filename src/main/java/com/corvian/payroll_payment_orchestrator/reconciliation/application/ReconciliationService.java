package com.corvian.payroll_payment_orchestrator.reconciliation.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.usecase.PayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.reconciliation.domain.ReconciliationStatus;
import com.corvian.payroll_payment_orchestrator.reconciliation.infrastructure.JpaReconciliationItemRepository;
import com.corvian.payroll_payment_orchestrator.reconciliation.infrastructure.ReconciliationItemEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {
    private final JpaReconciliationItemRepository repository;
    private final PayrollBatchUseCase payrollBatchUseCase;
    private final AuditLogService auditLogService;

    public ReconciliationService(JpaReconciliationItemRepository repository, PayrollBatchUseCase payrollBatchUseCase, AuditLogService auditLogService) {
        this.repository = repository;
        this.payrollBatchUseCase = payrollBatchUseCase;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReconciliationItemEntity reconcile(UUID batchId, String bankReference, BigDecimal bankAmount) {
        var batch = payrollBatchUseCase.findById(batchId);
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setId(UUID.randomUUID());
        item.setBatchId(batchId);
        item.setBankReference(bankReference);
        item.setExpectedAmount(batch.totalAmount());
        item.setBankAmount(bankAmount);
        item.setStatus(batch.totalAmount().compareTo(bankAmount) == 0 ? ReconciliationStatus.MATCHED : ReconciliationStatus.MISMATCHED);
        item.setCreatedAt(OffsetDateTime.now());
        ReconciliationItemEntity saved = repository.save(item);
        auditLogService.record("PAYROLL_BATCH_RECONCILED", "PAYROLL_BATCH", batchId, "Reconciliation status: " + saved.getStatus());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationItemEntity> findByBatch(UUID batchId) { return repository.findByBatchId(batchId); }
}
