package com.corvian.payroll_payment_orchestrator.companies.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaBankAccountRepository extends JpaRepository<BankAccountEntity, UUID> {
    List<BankAccountEntity> findByCompanyId(UUID companyId);
    Optional<BankAccountEntity> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByCompanyIdAndAccountNumberHash(UUID companyId, String accountNumberHash);
}
