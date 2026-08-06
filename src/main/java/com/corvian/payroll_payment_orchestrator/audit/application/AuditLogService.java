package com.corvian.payroll_payment_orchestrator.audit.application;

import com.corvian.payroll_payment_orchestrator.audit.infrastructure.AuditLogEntity;
import com.corvian.payroll_payment_orchestrator.audit.infrastructure.JpaAuditLogRepository;
import com.corvian.payroll_payment_orchestrator.shared.filter.RequestMetadataContext;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorContext;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorType;
import com.corvian.payroll_payment_orchestrator.shared.security.context.AuthenticatedActor;
import org.slf4j.MDC;
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
    private final ActorContext actorContext;
    private final RequestMetadataContext requestMetadataContext;

    public AuditLogService(
            JpaAuditLogRepository repository,
            ActorContext actorContext,
            RequestMetadataContext requestMetadataContext
    ) {
        this.repository = repository;
        this.actorContext = actorContext;
        this.requestMetadataContext = requestMetadataContext;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, UUID resourceId, String description) {
        AuthenticatedActor actor = currentActor();
        recordInternal(action, resourceType, resourceId, description, actor.subject(), actor.actorType().name(),
                metadata().correlationId(), metadata().clientIp(), actor.tenantId(), actor.companyId(),
                "SUCCESS", null, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, UUID resourceId, String description, UUID tenantId, UUID companyId) {
        AuthenticatedActor actor = currentActor();
        recordInternal(action, resourceType, resourceId, description, actor.subject(), actor.actorType().name(),
                metadata().correlationId(), metadata().clientIp(), tenantId, companyId,
                "SUCCESS", null, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String action, String resourceType, UUID resourceId, String description,
            String actor, String correlationId, String clientIp
    ) {
        recordInternal(action, resourceType, resourceId, description, actor, "SYSTEM", correlationId, clientIp,
                null, null, "SUCCESS", null, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String action, String resourceType, UUID resourceId, String description,
            String actor, String correlationId, String clientIp, UUID tenantId, UUID companyId
    ) {
        recordInternal(action, resourceType, resourceId, description, actor, "SYSTEM", correlationId, clientIp,
                tenantId, companyId, "SUCCESS", null, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordWithStateChange(
            String action, String resourceType, UUID resourceId, String description,
            String actor, String correlationId, String clientIp,
            UUID tenantId, UUID companyId, String entityType, UUID entityId,
            String oldStatus, String newStatus, String result
    ) {
        recordInternal(action, resourceType, resourceId, description, actor, currentActor().actorType().name(),
                correlationId, clientIp, tenantId, companyId, result, null, oldStatus, newStatus, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String action, String resourceType, UUID resourceId, String description,
            String actor, String correlationId, String clientIp, String failureReason
    ) {
        AuthenticatedActor current = currentActor();
        recordInternal(action, resourceType, resourceId, description, actor, current.actorType().name(),
                correlationId, clientIp, current.tenantId(), current.companyId(),
                "FAILURE", failureReason, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntity> findByResourceId(UUID resourceId) {
        AuthenticatedActor actor = currentActor();
        if (actor.platformAdmin() || actor.actorType() == ActorType.SYSTEM) {
            return repository.findByResourceIdOrderByCreatedAtDesc(resourceId);
        }
        return repository.findByResourceIdAndTenantIdOrderByCreatedAtDesc(resourceId, actor.tenantId());
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntity> findLatest() {
        AuthenticatedActor actor = currentActor();
        if (actor.platformAdmin() || actor.actorType() == ActorType.SYSTEM) return repository.findTop50ByOrderByCreatedAtDesc();
        if (actor.companyId() != null) return repository.findTop50ByCompanyIdOrderByCreatedAtDesc(actor.companyId());
        return repository.findTop50ByTenantIdOrderByCreatedAtDesc(actor.tenantId());
    }

    private void recordInternal(
            String action, String resourceType, UUID resourceId, String description,
            String actor, String actorType, String correlationId, String clientIp,
            UUID tenantId, UUID companyId, String result, String failureReason,
            String oldStatus, String newStatus, UUID eventId
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setAction(limit(action, 80));
        entity.setResourceType(limit(resourceType, 80));
        entity.setResourceId(resourceId);
        entity.setDescription(limit(description, 500));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setActor(defaultActor(actor));
        entity.setActorType(limit(actorType == null ? "SYSTEM" : actorType, 30));
        entity.setTenantId(tenantId);
        entity.setCompanyId(companyId);
        entity.setCorrelationId(limit(correlationId, 128));
        entity.setClientIp(limit(clientIp, 45));
        entity.setResult(limit(result == null ? "SUCCESS" : result, 30));
        entity.setFailureReason(limit(failureReason, 500));
        entity.setOldStatus(limit(oldStatus, 40));
        entity.setNewStatus(limit(newStatus, 40));
        entity.setTraceId(limit(MDC.get("traceId"), 64));
        entity.setEventId(eventId);
        repository.save(entity);
    }

    private AuthenticatedActor currentActor() {
        return actorContext.current();
    }

    private RequestMetadataContext.RequestMetadata metadata() {
        return requestMetadataContext.get();
    }

    private String defaultActor(String actor) {
        return actor == null || actor.isBlank() ? SYSTEM_ACTOR : limit(actor, 120);
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
