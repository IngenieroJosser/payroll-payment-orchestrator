package com.corvian.payroll_payment_orchestrator.companies.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCompanyRepository extends JpaRepository<CompanyEntity, UUID> {
    List<CompanyEntity> findByTenantId(UUID tenantId);
}
