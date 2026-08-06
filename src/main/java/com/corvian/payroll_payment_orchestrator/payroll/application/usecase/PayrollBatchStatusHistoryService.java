package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.history.JpaPayrollBatchStatusHistoryRepository;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.history.PayrollBatchStatusHistoryEntity;
import com.corvian.payroll_payment_orchestrator.shared.filter.RequestMetadataContext;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PayrollBatchStatusHistoryService {
    private final JpaPayrollBatchStatusHistoryRepository repository;
    private final ActorContext actorContext;
    private final RequestMetadataContext requestMetadataContext;

    public PayrollBatchStatusHistoryService(
            JpaPayrollBatchStatusHistoryRepository repository,
            ActorContext actorContext,
            RequestMetadataContext requestMetadataContext
    ) {
        this.repository = repository;
        this.actorContext = actorContext;
        this.requestMetadataContext = requestMetadataContext;
    }

    public void record(
            UUID tenantId,
            UUID companyId,
            UUID batchId,
            PayrollBatchStatus previousStatus,
            PayrollBatchStatus newStatus,
            String reason,
            OffsetDateTime changedAt
    ) {
        var actor = actorContext.current();
        PayrollBatchStatusHistoryEntity entity = new PayrollBatchStatusHistoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setCompanyId(companyId);
        entity.setBatchId(batchId);
        entity.setPreviousStatus(previousStatus);
        entity.setNewStatus(newStatus);
        entity.setChangedBy(actor.subject());
        entity.setActorType(actor.actorType().name());
        entity.setReason(limit(reason, 250));
        entity.setCorrelationId(requestMetadataContext.get().correlationId());
        entity.setChangedAt(changedAt);
        repository.save(entity);
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
