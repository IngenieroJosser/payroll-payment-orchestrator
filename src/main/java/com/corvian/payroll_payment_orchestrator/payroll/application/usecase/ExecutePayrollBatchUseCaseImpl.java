package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchMetadata;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollExecutionPublisherPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ExecutePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.PayrollBatchNotFoundException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ResourceAccessService;
import com.corvian.payroll_payment_orchestrator.shared.deployment.PaymentExecutionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ExecutePayrollBatchUseCaseImpl implements ExecutePayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final PayrollExecutionPublisherPort executionPublisher;
    private final AuditLogService auditLogService;
    private final ResourceAccessService accessService;
    private final PayrollBatchStatusHistoryService historyService;
    private final PaymentExecutionPolicy paymentExecutionPolicy;
    private final Clock clock;

    public ExecutePayrollBatchUseCaseImpl(
            PayrollBatchRepositoryPort repository,
            PayrollExecutionPublisherPort executionPublisher,
            AuditLogService auditLogService,
            ResourceAccessService accessService,
            PayrollBatchStatusHistoryService historyService,
            PaymentExecutionPolicy paymentExecutionPolicy,
            Clock clock
    ) {
        this.repository = repository;
        this.executionPublisher = executionPublisher;
        this.auditLogService = auditLogService;
        this.accessService = accessService;
        this.historyService = historyService;
        this.paymentExecutionPolicy = paymentExecutionPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PayrollBatch execute(UUID batchId) {
        paymentExecutionPolicy.requireEnabled();
        PayrollBatch batch = repository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
        PayrollBatchMetadata metadata = repository.findMetadata(batchId)
                .orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
        accessService.requireCompanyAccess(metadata.companyId());

        OffsetDateTime now = OffsetDateTime.now(clock);
        PayrollBatch processingBatch = batch.transitionTo(PayrollBatchStatus.PROCESSING, now)
                .copyWithPayments(paymentsWithStatus(batch, PayrollPaymentStatus.PROCESSING), now);
        PayrollBatch saved = repository.save(processingBatch);
        historyService.record(metadata.tenantId(), metadata.companyId(), batchId, batch.status(), saved.status(),
                "Payroll execution requested", now);
        auditLogService.record("PAYROLL_BATCH_EXECUTION_REQUESTED", "PAYROLL_BATCH", saved.id(),
                "Payroll batch execution requested", metadata.tenantId(), metadata.companyId());
        executionPublisher.publishExecutionRequested(saved.id());
        return saved;
    }

    private List<PayrollPayment> paymentsWithStatus(PayrollBatch batch, PayrollPaymentStatus status) {
        return batch.payments().stream().map(payment -> payment.withStatus(status)).toList();
    }
}
