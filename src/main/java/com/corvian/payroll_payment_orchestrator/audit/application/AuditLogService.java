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
    private final JpaAuditLogRepository repository;

    public AuditLogService(JpaAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, UUID resourceId, String description) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setAction(action);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setDescription(description);
        entity.setCreatedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntity> findByResourceId(UUID resourceId) {
        return repository.findByResourceIdOrderByCreatedAtDesc(resourceId);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntity> findLatest() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }
}
