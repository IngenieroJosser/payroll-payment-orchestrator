package com.corvian.payroll_payment_orchestrator.banks.infrastructure;
import com.corvian.payroll_payment_orchestrator.banks.domain.BankConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface JpaBankConnectionRepository extends JpaRepository<BankConnectionEntity, UUID> {
    Optional<BankConnectionEntity> findFirstByCompanyIdAndBankCodeAndStatus(UUID companyId, String bankCode, BankConnectionStatus status);
}
