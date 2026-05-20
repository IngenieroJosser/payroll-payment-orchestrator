package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.CreatePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
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
public class CreatePayrollBatchUseCaseImpl implements CreatePayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final AuditLogService auditLogService;

    public CreatePayrollBatchUseCaseImpl(PayrollBatchRepositoryPort repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
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
}
