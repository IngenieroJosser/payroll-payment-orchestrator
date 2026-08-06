package com.corvian.payroll_payment_orchestrator.banks.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.banks.application.model.*;
import com.corvian.payroll_payment_orchestrator.banks.domain.exception.BankProviderException;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.submission.BankSubmissionEntity;
import com.corvian.payroll_payment_orchestrator.banks.resilience.BankProviderCircuitBreaker;
import com.corvian.payroll_payment_orchestrator.observability.FinancialMetrics;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.BankAccountEntity;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.JpaBankAccountRepository;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.usecase.PayrollBatchService;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.PayrollBatchNotFoundException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class BankSubmissionCoordinator {
    private static final Logger log = LoggerFactory.getLogger(BankSubmissionCoordinator.class);
    private final PayrollBatchRepositoryPort batchRepository;
    private final JpaBankAccountRepository accountRepository;
    private final BankConnectionService connectionService;
    private final BankPaymentProviderResolver providerResolver;
    private final BankSubmissionStore submissionStore;
    private final PayrollBatchService payrollBatchService;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final BankProviderCircuitBreaker circuitBreaker;
    private final FinancialMetrics metrics;
    private final Duration pollDelay;
    private final Duration pollLease;
    private final int maxPollAttempts;

    public BankSubmissionCoordinator(
            PayrollBatchRepositoryPort batchRepository,
            JpaBankAccountRepository accountRepository,
            BankConnectionService connectionService,
            BankPaymentProviderResolver providerResolver,
            BankSubmissionStore submissionStore,
            PayrollBatchService payrollBatchService,
            AuditLogService auditLogService,
            Clock clock,
            BankProviderCircuitBreaker circuitBreaker,
            FinancialMetrics metrics,
            @Value("${app.bank.status-poll-delay-ms:30000}") long pollDelayMs,
            @Value("${app.bank.status-poll-lease-ms:360000}") long pollLeaseMs,
            @Value("${app.bank.max-status-poll-attempts:120}") int maxPollAttempts
    ) {
        this.batchRepository = batchRepository;
        this.accountRepository = accountRepository;
        this.connectionService = connectionService;
        this.providerResolver = providerResolver;
        this.submissionStore = submissionStore;
        this.payrollBatchService = payrollBatchService;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
        this.pollDelay = Duration.ofMillis(Math.max(1000, pollDelayMs));
        this.pollLease = Duration.ofMillis(Math.max(this.pollDelay.toMillis(), pollLeaseMs));
        this.maxPollAttempts = Math.max(1, maxPollAttempts);
    }

    public void submit(UUID batchId, UUID executionId, String correlationId) {
        PayrollBatch batch = loadBatch(batchId);
        var metadata = batchRepository.findMetadata(batchId).orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
        BankConnectionProfile profile = connectionService.resolveForSourceAccount(batch.companyId(), batch.sourceAccountId());
        BankPaymentProvider provider = providerResolver.resolve(profile.providerKey());
        validateCapabilities(provider.getCapabilities(), batch);

        BankSubmissionEntity submission = submissionStore.prepare(metadata.tenantId(), batch.companyId(), batch.id(),
                profile.id(), executionId, provider.providerKey(), "PAYROLL-" + batch.id());
        if (submission.getStatus() == BankSubmissionStatus.ACCEPTED
                || submission.getStatus() == BankSubmissionStatus.PROCESSING
                || submission.getStatus() == BankSubmissionStatus.PARTIALLY_SETTLED
                || submission.getStatus() == BankSubmissionStatus.SETTLED) {
            return;
        }

        BankAccountEntity source = accountRepository.findByIdAndCompanyId(batch.sourceAccountId(), batch.companyId())
                .orElseThrow(() -> new DomainException("BANK_ACCOUNT_NOT_FOUND", "Source bank account was not found"));
        BankSubmissionCommand command = new BankSubmissionCommand(metadata.tenantId(), batch.companyId(), batch.id(),
                submission.getExecutionId(), submission.getBankIdempotencyKey(), batch.currency(), batch.scheduledDate(),
                source.getAccountNumber(), batch.payments().stream().map(payment -> new BankPaymentInstruction(
                        payment.id(), payment.employeeFullName(), payment.employeeDocumentType(), payment.employeeDocumentNumber(),
                        payment.bankCode(), payment.accountType(), payment.accountNumber(), payment.amount(),
                        batch.id() + "-" + payment.id())).toList(), profile, correlationId);

        submissionStore.markSubmitting(submission.getId());
        try {
            BankSubmissionResult result = metrics.time("payroll.bank.submission.duration",
                    () -> circuitBreaker.execute(profile.id(), () -> provider.submitPayrollBatch(command)),
                    "provider", provider.providerKey(), "bank", profile.bankCode());
            submissionStore.recordSubmissionResult(submission.getId(), result, pollDelay);
            metrics.increment("payroll.bank.submissions", "provider", provider.providerKey(), "status", result.status().name());
            applySubmissionState(batch, result.status());
            auditLogService.record("BANK_SUBMISSION_ACCEPTED", "BANK_SUBMISSION", submission.getId(),
                    "Bank submission returned status " + result.status(), metadata.tenantId(), batch.companyId());
        } catch (BankProviderException ex) {
            if (ex.isRetryable()) {
                submissionStore.recordRetryableFailure(submission.getId(), ex.getCode(), ex.getMessage());
                throw ex;
            }
            submissionStore.recordTerminalFailure(submission.getId(), ex.getCode(), ex.getMessage());
            payrollBatchService.markAsFailed(batchId, ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.bank.status-poll-fixed-delay-ms:15000}")
    public void pollDueSubmissions() {
        for (UUID id : submissionStore.claimDueSubmissionIds(OffsetDateTime.now(clock), 50, pollLease)) {
            try { poll(id); }
            catch (Exception ex) { log.warn("Bank status polling failed. submissionId={}", id, ex); }
        }
    }

    public void poll(UUID submissionId) {
        BankSubmissionEntity submission = submissionStore.get(submissionId);
        if (submission.getAttemptCount() >= maxPollAttempts) {
            submissionStore.recordTerminalFailure(submissionId, "BANK_STATUS_POLL_EXHAUSTED", "Bank status polling attempts were exhausted");
            payrollBatchService.markAsFailed(submission.getBatchId(), "Bank status polling attempts were exhausted");
            return;
        }
        BankConnectionProfile profile = connectionService.getProfile(submission.getBankConnectionId());
        BankPaymentProvider provider = providerResolver.resolve(submission.getProviderKey());
        try {
            BankPaymentStatusResult result = metrics.time("payroll.bank.status.duration",
                    () -> circuitBreaker.execute(profile.id(), () -> provider.getBatchStatus(new BankStatusQuery(submissionId,
                            submission.getExternalBatchId(), profile, null))),
                    "provider", provider.providerKey(), "bank", profile.bankCode());
            submissionStore.recordStatus(submissionId, result.status(), result.payments(), pollDelay);
            metrics.increment("payroll.bank.status.checks", "provider", provider.providerKey(), "status", result.status().name());
            applyStatusResult(submission, result);
        } catch (BankProviderException ex) {
            if (ex.isRetryable()) {
                submissionStore.recordPollFailure(submissionId, ex.getCode(), ex.getMessage(), pollDelay);
                throw ex;
            }
            submissionStore.recordTerminalFailure(submissionId, ex.getCode(), ex.getMessage());
            payrollBatchService.markAsFailed(submission.getBatchId(), ex.getMessage());
        }
    }

    private void applySubmissionState(PayrollBatch batch, BankSubmissionStatus status) {
        switch (status) {
            case ACCEPTED, PROCESSING -> {
                if (batch.status() == PayrollBatchStatus.PROCESSING) payrollBatchService.markAsSentToBank(batch.id());
            }
            case SETTLED -> {
                if (batch.status() == PayrollBatchStatus.PROCESSING) payrollBatchService.markAsSentToBank(batch.id());
                payrollBatchService.markAsPaid(batch.id());
            }
            case REJECTED, FAILED -> payrollBatchService.markAsFailed(batch.id(), "Bank rejected the payroll submission");
            case PREPARED, SUBMITTING, PARTIALLY_SETTLED, UNKNOWN -> { }
        }
    }

    private void applyStatusResult(BankSubmissionEntity submission, BankPaymentStatusResult result) {
        PayrollBatch batch = loadBatch(submission.getBatchId());
        Map<UUID, PayrollPaymentStatus> statuses = new java.util.HashMap<>();
        for (BankPaymentResult paymentResult : result.payments()) {
            statuses.put(paymentResult.paymentId(), paymentResult.status());
        }
        switch (result.status()) {
            case SETTLED -> payrollBatchService.markAsPaid(batch.id());
            case PARTIALLY_SETTLED -> {
                if (!statuses.isEmpty()) payrollBatchService.markAsPartiallyPaid(batch.id(), statuses);
            }
            case REJECTED, FAILED -> payrollBatchService.markAsFailed(
                    batch.id(), "Bank reported terminal status " + result.status(), statuses);
            case ACCEPTED, PROCESSING -> {
                if (batch.status() == PayrollBatchStatus.PROCESSING) payrollBatchService.markAsSentToBank(batch.id());
            }
            case PREPARED, SUBMITTING, UNKNOWN -> { }
        }
    }

    @Transactional(readOnly = true)
    protected PayrollBatch loadBatch(UUID batchId) {
        return batchRepository.findById(batchId).orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
    }

    private void validateCapabilities(BankCapabilities capabilities, PayrollBatch batch) {
        if (!capabilities.batchSubmission()) throw new DomainException("BANK_CAPABILITY_UNSUPPORTED", "Bank adapter does not support batch submission");
        if (!capabilities.idempotency()) throw new DomainException("BANK_IDEMPOTENCY_UNSUPPORTED", "Bank adapter must support idempotent submissions");
        if (!capabilities.batchStatus()) throw new DomainException("BANK_STATUS_UNSUPPORTED", "Bank adapter must support normalized batch status queries");
        if (batch.totalPayments() > capabilities.maxPaymentsPerBatch()) throw new DomainException("BANK_BATCH_LIMIT_EXCEEDED", "Payroll exceeds bank adapter payment limit");
        if (!capabilities.supportedCurrencies().isEmpty() && !capabilities.supportedCurrencies().contains(batch.currency())) {
            throw new DomainException("BANK_CURRENCY_UNSUPPORTED", "Bank adapter does not support payroll currency");
        }
    }
}
