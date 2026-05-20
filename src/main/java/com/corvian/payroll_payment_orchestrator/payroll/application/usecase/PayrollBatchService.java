package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.CreatePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ApprovePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ExecutePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.PayrollBatchNotFoundException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.webhooks.application.WebhookDeliveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PayrollBatchService implements PayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final AuditLogService auditLogService;
    private final WebhookDeliveryService webhookDeliveryService;

    private final CreatePayrollBatchUseCase createUseCase;
    private final ApprovePayrollBatchUseCase approveUseCase;
    private final ExecutePayrollBatchUseCase executeUseCase;

    public PayrollBatchService(
            PayrollBatchRepositoryPort repository,
            AuditLogService auditLogService,
            WebhookDeliveryService webhookDeliveryService,
            CreatePayrollBatchUseCase createUseCase,
            ApprovePayrollBatchUseCase approveUseCase,
            ExecutePayrollBatchUseCase executeUseCase
    ) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.webhookDeliveryService = webhookDeliveryService;
        this.createUseCase = createUseCase;
        this.approveUseCase = approveUseCase;
        this.executeUseCase = executeUseCase;
    }

    @Override
    @Transactional
    public PayrollBatch create(CreatePayrollBatchCommand command) {
        return createUseCase.create(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollBatch> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollBatch findById(UUID batchId) {
        return repository.findById(batchId).orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
    }

    @Override
    @Transactional
    public PayrollBatch validate(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        PayrollBatch updated = repository.save(batch.transitionTo(PayrollBatchStatus.PENDING_APPROVAL));
        auditLogService.record("PAYROLL_BATCH_VALIDATED", "PAYROLL_BATCH", updated.id(), "Payroll batch validated and sent to approval");
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch approve(UUID batchId) {
        return approveUseCase.approve(batchId);
    }

    @Override
    @Transactional
    public PayrollBatch reject(UUID batchId, String reason) {
        return approveUseCase.reject(batchId, reason);
    }

    @Override
    @Transactional
    public PayrollBatch execute(UUID batchId) {
        return executeUseCase.execute(batchId);
    }

    @Override
    @Transactional
    public PayrollBatch markAsSentToBank(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        PayrollBatch updated = repository.save(batch.transitionTo(PayrollBatchStatus.SENT_TO_BANK));
        auditLogService.record("PAYROLL_BATCH_SENT_TO_BANK", "PAYROLL_BATCH", updated.id(), "Payroll batch sent to sandbox bank provider");
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch markAsPaid(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        PayrollBatch paidBatch = batch.transitionTo(PayrollBatchStatus.PAID);
        paidBatch = paidBatch.copyWithPayments(paymentsWithStatus(paidBatch, PayrollPaymentStatus.PAID));
        PayrollBatch updated = repository.save(paidBatch);
        auditLogService.record("PAYROLL_BATCH_PAID", "PAYROLL_BATCH", updated.id(), "Payroll batch marked as paid");
        webhookDeliveryService.publish(updated.companyId(), "payroll.batch.paid", updated.id(), updated);
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch markAsFailed(UUID batchId, String reason) {
        PayrollBatch batch = findById(batchId);
        PayrollBatch failedBatch = batch.transitionTo(PayrollBatchStatus.FAILED);
        failedBatch = failedBatch.copyWithPayments(paymentsWithStatus(failedBatch, PayrollPaymentStatus.FAILED));
        PayrollBatch updated = repository.save(failedBatch);
        auditLogService.record("PAYROLL_BATCH_FAILED", "PAYROLL_BATCH", updated.id(), "Payroll batch failed. Reason: " + reason);
        webhookDeliveryService.publish(updated.companyId(), "payroll.batch.failed", updated.id(), updated);
        return updated;
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
