package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchMetadata;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ApprovePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.CreatePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ExecutePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.PayrollBatchNotFoundException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorType;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ResourceAccessService;
import com.corvian.payroll_payment_orchestrator.webhooks.application.WebhookDeliveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PayrollBatchService implements PayrollBatchUseCase {
    private final PayrollBatchRepositoryPort repository;
    private final AuditLogService auditLogService;
    private final WebhookDeliveryService webhookDeliveryService;
    private final CreatePayrollBatchUseCase createUseCase;
    private final ApprovePayrollBatchUseCase approveUseCase;
    private final ExecutePayrollBatchUseCase executeUseCase;
    private final ResourceAccessService accessService;
    private final PayrollBatchStatusHistoryService historyService;
    private final Clock clock;

    public PayrollBatchService(
            PayrollBatchRepositoryPort repository,
            AuditLogService auditLogService,
            WebhookDeliveryService webhookDeliveryService,
            CreatePayrollBatchUseCase createUseCase,
            ApprovePayrollBatchUseCase approveUseCase,
            ExecutePayrollBatchUseCase executeUseCase,
            ResourceAccessService accessService,
            PayrollBatchStatusHistoryService historyService,
            Clock clock
    ) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.webhookDeliveryService = webhookDeliveryService;
        this.createUseCase = createUseCase;
        this.approveUseCase = approveUseCase;
        this.executeUseCase = executeUseCase;
        this.accessService = accessService;
        this.historyService = historyService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PayrollBatch create(CreatePayrollBatchCommand command) { return createUseCase.create(command); }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollBatch> findAll() {
        var actor = accessService.currentActor();
        if (actor.platformAdmin() || actor.actorType() == ActorType.SYSTEM) return repository.findAll();
        if (actor.companyId() != null) return repository.findByCompanyId(actor.companyId());
        if (actor.tenantId() != null) return repository.findByTenantId(actor.tenantId());
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollBatch findById(UUID batchId) {
        PayrollBatch batch = repository.findById(batchId).orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
        requireAccess(batch.companyId());
        return batch;
    }

    @Override
    @Transactional
    public PayrollBatch validate(UUID batchId) {
        PayrollBatch batch = locked(batchId);
        PayrollBatchMetadata metadata = metadata(batchId);
        requireAccess(metadata.companyId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        PayrollBatch current = transition(batch, PayrollBatchStatus.VALIDATING, metadata, "Payroll validation started", now);
        current = transition(current, PayrollBatchStatus.VALIDATED, metadata, "Payroll validation completed", now.plusNanos(1));
        current = transition(current, PayrollBatchStatus.PENDING_APPROVAL, metadata, "Payroll sent for approval", now.plusNanos(2));
        auditLogService.record("PAYROLL_BATCH_VALIDATED", "PAYROLL_BATCH", current.id(),
                "Payroll batch validated and sent to approval", metadata.tenantId(), metadata.companyId());
        return current;
    }

    @Override
    @Transactional
    public PayrollBatch approve(UUID batchId) { return approveUseCase.approve(batchId); }

    @Override
    @Transactional
    public PayrollBatch reject(UUID batchId, String reason) { return approveUseCase.reject(batchId, reason); }

    @Override
    @Transactional
    public PayrollBatch execute(UUID batchId) { return executeUseCase.execute(batchId); }

    @Override
    @Transactional
    public PayrollBatch markAsSentToBank(UUID batchId) {
        PayrollBatch batch = locked(batchId);
        PayrollBatchMetadata metadata = metadata(batchId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        PayrollBatch withPayments = batch.copyWithPayments(paymentsWithStatus(batch, PayrollPaymentStatus.SENT_TO_BANK), now);
        PayrollBatch updated = transition(withPayments, PayrollBatchStatus.SENT_TO_BANK, metadata,
                "Bank accepted payroll submission for processing", now);
        auditLogService.record("PAYROLL_BATCH_SENT_TO_BANK", "PAYROLL_BATCH", updated.id(),
                "Payroll batch accepted by bank provider", metadata.tenantId(), metadata.companyId());
        publishSafeEvent(updated, "payroll.batch.sent_to_bank");
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch markAsPaid(UUID batchId) {
        PayrollBatch batch = locked(batchId);
        PayrollBatchMetadata metadata = metadata(batchId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        PayrollBatch withPayments = batch.copyWithPayments(paymentsWithStatus(batch, PayrollPaymentStatus.PAID), now);
        PayrollBatch updated = transition(withPayments, PayrollBatchStatus.PAID, metadata,
                "All payroll payments confirmed settled by bank", now);
        auditLogService.record("PAYROLL_BATCH_PAID", "PAYROLL_BATCH", updated.id(),
                "Payroll batch settled by bank confirmation", metadata.tenantId(), metadata.companyId());
        publishSafeEvent(updated, "payroll.batch.paid");
        return updated;
    }

    @Transactional
    public PayrollBatch markAsPartiallyPaid(UUID batchId, Map<UUID, PayrollPaymentStatus> statuses) {
        PayrollBatch batch = locked(batchId);
        PayrollBatchMetadata metadata = metadata(batchId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<PayrollPayment> payments = batch.payments().stream()
                .map(payment -> payment.withStatus(statuses.getOrDefault(payment.id(), payment.status())))
                .toList();
        PayrollBatch withPayments = batch.copyWithPayments(payments, now);
        PayrollBatch updated = transition(withPayments, PayrollBatchStatus.PARTIALLY_PAID, metadata,
                "Bank reported a partial payroll settlement", now);
        publishSafeEvent(updated, "payroll.batch.partially_paid");
        return updated;
    }

    @Override
    @Transactional
    public PayrollBatch markAsFailed(UUID batchId, String reason) {
        return markAsFailed(batchId, reason, Map.of());
    }

    @Transactional
    public PayrollBatch markAsFailed(UUID batchId, String reason, Map<UUID, PayrollPaymentStatus> reportedStatuses) {
        PayrollBatch batch = locked(batchId);
        PayrollBatchMetadata metadata = metadata(batchId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<PayrollPayment> payments = batch.payments().stream()
                .map(payment -> payment.withStatus(failureStatus(payment, reportedStatuses)))
                .toList();
        PayrollBatch withPayments = batch.copyWithPayments(payments, now);
        PayrollBatch updated = transition(withPayments, PayrollBatchStatus.FAILED, metadata, sanitize(reason), now);
        auditLogService.record("PAYROLL_BATCH_FAILED", "PAYROLL_BATCH", updated.id(),
                "Payroll batch failed: " + sanitize(reason), metadata.tenantId(), metadata.companyId());
        publishSafeEvent(updated, "payroll.batch.failed");
        return updated;
    }

    private PayrollBatch transition(PayrollBatch batch, PayrollBatchStatus target, PayrollBatchMetadata metadata, String reason, OffsetDateTime at) {
        PayrollBatch updated = repository.save(batch.transitionTo(target, at));
        historyService.record(metadata.tenantId(), metadata.companyId(), batch.id(), batch.status(), target, reason, at);
        return updated;
    }

    private PayrollBatch locked(UUID batchId) {
        return repository.findByIdForUpdate(batchId).orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
    }

    private PayrollBatchMetadata metadata(UUID batchId) {
        return repository.findMetadata(batchId).orElseThrow(() -> new PayrollBatchNotFoundException(batchId));
    }

    private void requireAccess(UUID companyId) {
        accessService.requireCompanyAccess(companyId);
    }

    private List<PayrollPayment> paymentsWithStatus(PayrollBatch batch, PayrollPaymentStatus status) {
        return batch.payments().stream().map(payment -> payment.withStatus(status)).toList();
    }

    private PayrollPaymentStatus failureStatus(PayrollPayment payment, Map<UUID, PayrollPaymentStatus> reportedStatuses) {
        PayrollPaymentStatus reported = reportedStatuses.get(payment.id());
        if (reported != null) return reported;
        return switch (payment.status()) {
            case PAID, REJECTED, RETURNED -> payment.status();
            default -> PayrollPaymentStatus.FAILED;
        };
    }

    private void publishSafeEvent(PayrollBatch batch, String event) {
        webhookDeliveryService.publish(batch.companyId(), event, batch.id(), Map.of(
                "batchId", batch.id(),
                "companyId", batch.companyId(),
                "status", batch.status().name(),
                "currency", batch.currency(),
                "totalAmount", batch.totalAmount(),
                "totalPayments", batch.totalPayments(),
                "updatedAt", batch.updatedAt()
        ));
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "Unspecified processing failure";
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= 400 ? normalized : normalized.substring(0, 400);
    }
}
