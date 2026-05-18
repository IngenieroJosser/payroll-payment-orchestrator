package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollExecutionPublisherPort;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.InvalidPayrollBatchStateException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.PayrollBatchNotFoundException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.webhooks.application.WebhookDeliveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PayrollBatchService implements PayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final PayrollExecutionPublisherPort executionPublisher;
    private final AuditLogService auditLogService;
    private final WebhookDeliveryService webhookDeliveryService;

    public PayrollBatchService(
            PayrollBatchRepositoryPort repository,
            PayrollExecutionPublisherPort executionPublisher,
            AuditLogService auditLogService,
            WebhookDeliveryService webhookDeliveryService
    ) {
        this.repository = repository;
        this.executionPublisher = executionPublisher;
        this.auditLogService = auditLogService;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @Override
    @Transactional
    public PayrollBatch create(CreatePayrollBatchCommand command) {
        if (command.payments() == null || command.payments().isEmpty()) {
            throw new DomainException("EMPTY_PAYROLL_BATCH", "Payroll batch must contain at least one payment");
        }
        if (command.companyId() == null || command.sourceAccountId() == null) {
            throw new DomainException("INVALID_PAYROLL_BATCH", "Company and source account are required");
        }

        List<PayrollPayment> payments = command.payments().stream()
                .map(payment -> {
                    if (payment.amount() == null || payment.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new DomainException("INVALID_PAYMENT_AMOUNT", "Payment amount must be greater than zero");
                    }
                    return new PayrollPayment(
                            UUID.randomUUID(),
                            payment.employeeDocumentType(),
                            payment.employeeDocumentNumber(),
                            payment.employeeFullName(),
                            payment.bankCode(),
                            payment.accountType(),
                            payment.accountNumber(),
                            payment.amount(),
                            PayrollPaymentStatus.PENDING
                    );
                })
                .toList();

        BigDecimal totalAmount = payments.stream()
                .map(PayrollPayment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OffsetDateTime now = OffsetDateTime.now();
        PayrollBatch payrollBatch = new PayrollBatch(
                UUID.randomUUID(),
                command.companyId(),
                command.sourceAccountId(),
                command.currency().trim().toUpperCase(),
                command.scheduledDate(),
                PayrollBatchStatus.DRAFT,
                totalAmount,
                payments.size(),
                payments,
                now,
                now
        );

        PayrollBatch saved = repository.save(payrollBatch);
        auditLogService.record("PAYROLL_BATCH_CREATED", "PAYROLL_BATCH", saved.id(), "Payroll batch created with " + saved.totalPayments() + " payments");
        return saved;
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
        ensureStatus(batch, PayrollBatchStatus.DRAFT, "validate");
        PayrollBatch updated = repository.save(copyWithStatus(batch, PayrollBatchStatus.PENDING_APPROVAL, batch.payments()));
        auditLogService.record("PAYROLL_BATCH_VALIDATED", "PAYROLL_BATCH", updated.id(), "Payroll batch validated and sent to approval");
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch approve(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        ensureStatus(batch, PayrollBatchStatus.PENDING_APPROVAL, "approve");
        PayrollBatch updated = repository.save(copyWithStatus(batch, PayrollBatchStatus.APPROVED, batch.payments()));
        auditLogService.record("PAYROLL_BATCH_APPROVED", "PAYROLL_BATCH", updated.id(), "Payroll batch approved");
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch reject(UUID batchId, String reason) {
        PayrollBatch batch = findById(batchId);
        ensureStatus(batch, PayrollBatchStatus.PENDING_APPROVAL, "reject");
        PayrollBatch updated = repository.save(copyWithStatus(batch, PayrollBatchStatus.REJECTED, batch.payments()));
        auditLogService.record("PAYROLL_BATCH_REJECTED", "PAYROLL_BATCH", updated.id(), "Payroll batch rejected. Reason: " + reason);
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch execute(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        ensureStatus(batch, PayrollBatchStatus.APPROVED, "execute");
        PayrollBatch processingBatch = repository.save(copyWithStatus(batch, PayrollBatchStatus.PROCESSING, paymentsWithStatus(batch, PayrollPaymentStatus.PROCESSING)));
        auditLogService.record("PAYROLL_BATCH_EXECUTION_REQUESTED", "PAYROLL_BATCH", processingBatch.id(), "Payroll batch execution requested");
        executionPublisher.publishExecutionRequested(processingBatch.id());
        return processingBatch;
    }

    @Override
    @Transactional
    public PayrollBatch markAsSentToBank(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        ensureStatus(batch, PayrollBatchStatus.PROCESSING, "mark as sent to bank");
        PayrollBatch updated = repository.save(copyWithStatus(batch, PayrollBatchStatus.SENT_TO_BANK, batch.payments()));
        auditLogService.record("PAYROLL_BATCH_SENT_TO_BANK", "PAYROLL_BATCH", updated.id(), "Payroll batch sent to sandbox bank provider");
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch markAsPaid(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        if (batch.status() != PayrollBatchStatus.SENT_TO_BANK && batch.status() != PayrollBatchStatus.PROCESSING) {
            throw new InvalidPayrollBatchStateException(batch.status(), "mark as paid");
        }
        PayrollBatch updated = repository.save(copyWithStatus(batch, PayrollBatchStatus.PAID, paymentsWithStatus(batch, PayrollPaymentStatus.PAID)));
        auditLogService.record("PAYROLL_BATCH_PAID", "PAYROLL_BATCH", updated.id(), "Payroll batch marked as paid");
        webhookDeliveryService.publish(updated.companyId(), "payroll.batch.paid", updated.id(), updated);
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch markAsFailed(UUID batchId, String reason) {
        PayrollBatch batch = findById(batchId);
        PayrollBatch updated = repository.save(copyWithStatus(batch, PayrollBatchStatus.FAILED, paymentsWithStatus(batch, PayrollPaymentStatus.FAILED)));
        auditLogService.record("PAYROLL_BATCH_FAILED", "PAYROLL_BATCH", updated.id(), "Payroll batch failed. Reason: " + reason);
        webhookDeliveryService.publish(updated.companyId(), "payroll.batch.failed", updated.id(), updated);
        return updated;
    }

    private void ensureStatus(PayrollBatch batch, PayrollBatchStatus expected, String action) {
        if (batch.status() != expected) {
            throw new InvalidPayrollBatchStateException(batch.status(), action);
        }
    }

    private PayrollBatch copyWithStatus(PayrollBatch batch, PayrollBatchStatus status, List<PayrollPayment> payments) {
        return new PayrollBatch(
                batch.id(),
                batch.companyId(),
                batch.sourceAccountId(),
                batch.currency(),
                batch.scheduledDate(),
                status,
                batch.totalAmount(),
                batch.totalPayments(),
                payments,
                batch.createdAt(),
                OffsetDateTime.now()
        );
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
