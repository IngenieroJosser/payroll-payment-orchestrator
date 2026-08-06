package com.corvian.payroll_payment_orchestrator.iam.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaApiClientRepository extends JpaRepository<ApiClientEntity, UUID> {
    Optional<ApiClientEntity> findByClientId(String clientId);
    List<ApiClientEntity> findByTenantId(UUID tenantId);
    List<ApiClientEntity> findByCompanyId(UUID companyId);
}
