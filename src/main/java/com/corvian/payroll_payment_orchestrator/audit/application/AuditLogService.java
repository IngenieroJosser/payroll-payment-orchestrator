package com.corvian.payroll_payment_orchestrator.audit.application;

import com.corvian.payroll_payment_orchestrator.audit.infrastructure.AuditLogEntity;
import com.corvian.payroll_payment_orchestrator.audit.infrastructure.JpaAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final JpaAuditLogRepository repository;

    public AuditLogService(JpaAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, UUID resourceId, String description) {
        record(action, resourceType, resourceId, description, SYSTEM_ACTOR, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String action,
            String resourceType,
            UUID resourceId,
            String description,
            String actor,
            String correlationId,
            String clientIp
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setAction(limit(action, 80));
        entity.setResourceType(limit(resourceType, 80));
        entity.setResourceId(resourceId);
        entity.setDescription(limit(description, 500));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setActor(defaultActor(actor));
        entity.setCorrelationId(limit(correlationId, 128));
        entity.setClientIp(limit(clientIp, 45));
        repository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        String action,
        String resourceType,
        UUID resourceId,
        String description,
        String actor,
        String correlationId,
        String clientIp,
        UUID tenantId,
        UUID companyId
    ) {
        record(action, resourceType, resourceId, description, actor, correlationId, clientIp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordWithStateChange(
        String action,
        String resourceType,
        UUID resourceId,
        String description,
        String actor,
        String correlationId,
        String clientIp,
        UUID tenantId,
        UUID companyId,
        String entityType,
        UUID entityId,
        String oldStatus,
        String newStatus,
        String result
    ) {
        record(action, resourceType, resourceId, description, actor, correlationId, clientIp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
        String action,
        String resourceType,
        UUID resourceId,
        String description,
        String actor,
        String correlationId,
        String clientIp,
        String failureReason
    ) {
        record(action, resourceType, resourceId, description, actor, correlationId, clientIp);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntity> findByResourceId(UUID resourceId) {
        return repository.findByResourceIdOrderByCreatedAtDesc(resourceId);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntity> findLatest() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }

    private String defaultActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return SYSTEM_ACTOR;
        }
        return limit(actor, 120);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
