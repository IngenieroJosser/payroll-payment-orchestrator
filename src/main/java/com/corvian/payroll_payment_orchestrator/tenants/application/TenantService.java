package com.corvian.payroll_payment_orchestrator.tenants.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.tenants.infrastructure.JpaTenantRepository;
import com.corvian.payroll_payment_orchestrator.tenants.infrastructure.TenantEntity;
import com.corvian.payroll_payment_orchestrator.tenants.infrastructure.TenantStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TenantService {
    private final JpaTenantRepository repository;
    private final AuditLogService auditLogService;

    public TenantService(JpaTenantRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TenantEntity create(String name, String slug) {
        String normalizedSlug = slug.trim().toLowerCase();
        if (repository.existsBySlug(normalizedSlug)) {
            throw new DomainException("TENANT_SLUG_ALREADY_EXISTS", "Tenant slug already exists");
        }
        OffsetDateTime now = OffsetDateTime.now();
        TenantEntity entity = new TenantEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name.trim());
        entity.setSlug(normalizedSlug);
        entity.setStatus(TenantStatus.ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        TenantEntity saved = repository.save(entity);
        auditLogService.record("TENANT_CREATED", "TENANT", saved.getId(), "Tenant created: " + saved.getSlug());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TenantEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public TenantEntity findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DomainException("TENANT_NOT_FOUND", "Tenant was not found"));
    }
}
