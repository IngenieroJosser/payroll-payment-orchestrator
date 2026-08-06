package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.companies.application.CompanyService;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.BankAccountEntity;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.CompanyEntity;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.CreatePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.config.PayrollProperties;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CreatePayrollBatchUseCaseImpl implements CreatePayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final AuditLogService auditLogService;
    private final CompanyService companyService;
    private final PayrollBatchStatusHistoryService historyService;
    private final PayrollProperties properties;
    private final Clock clock;

    public CreatePayrollBatchUseCaseImpl(
            PayrollBatchRepositoryPort repository,
            AuditLogService auditLogService,
            CompanyService companyService,
            PayrollBatchStatusHistoryService historyService,
            PayrollProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.companyService = companyService;
        this.historyService = historyService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PayrollBatch create(CreatePayrollBatchCommand command) {
        requireValidCommand(command);
        CompanyEntity company = companyService.findById(command.companyId());
        BankAccountEntity sourceAccount = companyService.requireActiveBankAccount(command.companyId(), command.sourceAccountId());
        String currency = normalizeCurrency(command.currency());
        if (!company.getCurrency().equals(currency)) {
            throw new DomainException("CURRENCY_MISMATCH", "Payroll currency must match the configured company currency");
        }
        if (!sourceAccount.getCompanyId().equals(command.companyId())) {
            throw new DomainException("SOURCE_ACCOUNT_MISMATCH", "Source bank account does not belong to the payroll company");
        }
        if (command.scheduledDate().isBefore(LocalDate.now(clock))) {
            throw new DomainException("INVALID_SCHEDULED_DATE", "Scheduled date cannot be in the past");
        }

        List<PayrollPayment> payments = command.payments().stream()
                .map(payment -> new PayrollPayment(
                        UUID.randomUUID(),
                        payment.employeeDocumentType(),
                        payment.employeeDocumentNumber(),
                        payment.employeeFullName(),
                        payment.bankCode(),
                        payment.accountType(),
                        payment.accountNumber(),
                        payment.amount(),
                        PayrollPaymentStatus.PENDING
                ))
                .toList();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PayrollPayment payment : payments) {
            totalAmount = totalAmount.add(payment.amount());
        }
        if (totalAmount.compareTo(properties.getMaxBatchAmount()) > 0) {
            throw new DomainException("PAYROLL_BATCH_AMOUNT_LIMIT_EXCEEDED", "Payroll batch exceeds the configured maximum amount");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        PayrollBatch saved = repository.save(new PayrollBatch(
                UUID.randomUUID(), command.companyId(), command.sourceAccountId(), currency,
                command.scheduledDate(), PayrollBatchStatus.DRAFT, totalAmount, payments.size(), payments, now, now
        ));
        UUID tenantId = repository.findMetadata(saved.id())
                .orElseThrow(() -> new IllegalStateException("Payroll metadata was not persisted"))
                .tenantId();
        historyService.record(tenantId, saved.companyId(), saved.id(), null, PayrollBatchStatus.DRAFT,
                "Payroll batch created", now);
        auditLogService.record("PAYROLL_BATCH_CREATED", "PAYROLL_BATCH", saved.id(),
                "Payroll batch created with " + saved.totalPayments() + " payments", tenantId, saved.companyId());
        return saved;
    }

    private void requireValidCommand(CreatePayrollBatchCommand command) {
        if (command == null || command.companyId() == null || command.sourceAccountId() == null) {
            throw new DomainException("INVALID_PAYROLL_BATCH", "Company and source account are required");
        }
        if (command.payments() == null || command.payments().isEmpty()) {
            throw new DomainException("EMPTY_PAYROLL_BATCH", "Payroll batch must contain at least one payment");
        }
        if (command.payments().size() > properties.getMaxPaymentsPerBatch()) {
            throw new DomainException("PAYROLL_BATCH_SIZE_LIMIT_EXCEEDED", "Payroll batch exceeds the configured payment limit");
        }
    }

    private String normalizeCurrency(String value) {
        try {
            return Currency.getInstance(value.trim().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (Exception ex) {
            throw new DomainException("INVALID_CURRENCY", "Currency must be a valid ISO-4217 code");
        }
    }
}
