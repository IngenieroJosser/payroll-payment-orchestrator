package com.corvian.payroll_payment_orchestrator.shared.security.context;

import com.corvian.payroll_payment_orchestrator.companies.infrastructure.CompanyEntity;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.JpaCompanyRepository;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ResourceAccessService {
    private final ActorContext actorContext;
    private final JpaCompanyRepository companyRepository;

    public ResourceAccessService(ActorContext actorContext, JpaCompanyRepository companyRepository) {
        this.actorContext = actorContext;
        this.companyRepository = companyRepository;
    }

    public AuthenticatedActor currentActor() {
        return actorContext.current();
    }

    public void requirePlatformAdministration() {
        AuthenticatedActor actor = actorContext.current();
        if (!actor.platformAdmin() && actor.actorType() != ActorType.SYSTEM) {
            throw new DomainException("PLATFORM_ADMIN_REQUIRED", "This operation requires platform administrator privileges");
        }
    }

    public void requireTenantAccess(UUID tenantId) {
        if (tenantId == null) {
            throw new DomainException("TENANT_REQUIRED", "Tenant context is required");
        }
        AuthenticatedActor actor = actorContext.current();
        if (actor.platformAdmin() || actor.actorType() == ActorType.SYSTEM) {
            return;
        }
        if (!tenantId.equals(actor.tenantId())) {
            throw new DomainException("RESOURCE_ACCESS_DENIED", "The requested resource does not belong to the authenticated tenant");
        }
    }

    @Transactional(readOnly = true)
    public CompanyEntity requireCompanyAccess(UUID companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new DomainException("COMPANY_NOT_FOUND", "Company was not found"));
        requireCompanyAccess(company);
        return company;
    }

    public void requireCompanyAccess(CompanyEntity company) {
        AuthenticatedActor actor = actorContext.current();
        if (actor.platformAdmin() || actor.actorType() == ActorType.SYSTEM) {
            return;
        }
        if (!company.getTenantId().equals(actor.tenantId())) {
            throw new DomainException("RESOURCE_ACCESS_DENIED", "The requested company does not belong to the authenticated tenant");
        }
        if (actor.companyId() != null && !company.getId().equals(actor.companyId())) {
            throw new DomainException("RESOURCE_ACCESS_DENIED", "The requested company is outside the authenticated company scope");
        }
    }
}
