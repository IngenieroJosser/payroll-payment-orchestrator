package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

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

    public PayrollBatchService(PayrollBatchRepositoryPort repository, PayrollExecutionPublisherPort executionPublisher) {
        this.repository = repository;
        this.executionPublisher = executionPublisher;
    }

    @Override
    @Transactional
    public PayrollBatch create(CreatePayrollBatchCommand command) {
        if (command.payments() == null || command.payments().isEmpty()) {
            throw new DomainException("EMPTY_PAYROLL_BATCH", "Payroll batch must contain at least one payment");
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
                command.currency(),
                command.scheduledDate(),
                PayrollBatchStatus.DRAFT,
                totalAmount,
                payments.size(),
                payments,
                now,
                now
        );

        return repository.save(payrollBatch);
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
        if (batch.status() != PayrollBatchStatus.DRAFT) {
            throw new InvalidPayrollBatchStateException(batch.status(), "validate");
        }

        return repository.save(copyWithStatus(batch, PayrollBatchStatus.PENDING_APPROVAL));
    }

    @Override
    @Transactional
    public PayrollBatch approve(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        if (batch.status() != PayrollBatchStatus.PENDING_APPROVAL) {
            throw new InvalidPayrollBatchStateException(batch.status(), "approve");
        }

        return repository.save(copyWithStatus(batch, PayrollBatchStatus.APPROVED));
    }

    @Override
    @Transactional
    public PayrollBatch execute(UUID batchId) {
        PayrollBatch batch = findById(batchId);
        if (batch.status() != PayrollBatchStatus.APPROVED) {
            throw new InvalidPayrollBatchStateException(batch.status(), "execute");
        }

        PayrollBatch processingBatch = repository.save(copyWithStatus(batch, PayrollBatchStatus.PROCESSING));
        executionPublisher.publishExecutionRequested(processingBatch.id());
        return processingBatch;
    }

    private PayrollBatch copyWithStatus(PayrollBatch batch, PayrollBatchStatus status) {
        return new PayrollBatch(
                batch.id(),
                batch.companyId(),
                batch.sourceAccountId(),
                batch.currency(),
                batch.scheduledDate(),
                status,
                batch.totalAmount(),
                batch.totalPayments(),
                batch.payments(),
                batch.createdAt(),
                OffsetDateTime.now()
        );
    }
}
