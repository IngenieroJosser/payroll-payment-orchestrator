package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ApprovePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.PayrollBatchNotFoundException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ApprovePayrollBatchUseCaseImpl implements ApprovePayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final AuditLogService auditLogService;

    public ApprovePayrollBatchUseCaseImpl(PayrollBatchRepositoryPort repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public PayrollBatch approve(UUID batchId) {
        PayrollBatch batch = repository.findById(batchId)
                .orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
        PayrollBatch updated = repository.save(batch.transitionTo(PayrollBatchStatus.APPROVED));
        auditLogService.record("PAYROLL_BATCH_APPROVED", "PAYROLL_BATCH", updated.id(), "Payroll batch approved");
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch reject(UUID batchId, String reason) {
        PayrollBatch batch = repository.findById(batchId)
                .orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
        PayrollBatch updated = repository.save(batch.transitionTo(PayrollBatchStatus.REJECTED));
        auditLogService.record("PAYROLL_BATCH_REJECTED", "PAYROLL_BATCH", updated.id(), "Payroll batch rejected. Reason: " + reason);
        return updated;
    }
}
