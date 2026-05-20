package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollExecutionPublisherPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ExecutePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.PayrollBatchNotFoundException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExecutePayrollBatchUseCaseImpl implements ExecutePayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final PayrollExecutionPublisherPort executionPublisher;
    private final AuditLogService auditLogService;

    public ExecutePayrollBatchUseCaseImpl(
            PayrollBatchRepositoryPort repository,
            PayrollExecutionPublisherPort executionPublisher,
            AuditLogService auditLogService
    ) {
        this.repository = repository;
        this.executionPublisher = executionPublisher;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public PayrollBatch execute(UUID batchId) {
        PayrollBatch batch = repository.findById(batchId)
                .orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
        PayrollBatch processingBatch = batch.transitionTo(PayrollBatchStatus.PROCESSING);
        processingBatch = processingBatch.copyWithPayments(paymentsWithStatus(processingBatch, PayrollPaymentStatus.PROCESSING));
        PayrollBatch saved = repository.save(processingBatch);
        auditLogService.record("PAYROLL_BATCH_EXECUTION_REQUESTED", "PAYROLL_BATCH", saved.id(), "Payroll batch execution requested");
        executionPublisher.publishExecutionRequested(saved.id());
        return saved;
    }

    private List<PayrollPayment> paymentsWithStatus(PayrollBatch batch, PayrollPaymentStatus status) {
        return batch.payments().stream()
                .map(payment -> new PayrollPayment(
                        payment.id(),
                        payment.employeeDocumentType(),
                        payment.employeeDocumentNumber(),
                        payment.employeeFullName(),
                        payment.bankCode(),
                        payment.accountType(),
                        payment.accountNumber(),
                        payment.amount(),
                        status
                ))
                .toList();
    }
}
