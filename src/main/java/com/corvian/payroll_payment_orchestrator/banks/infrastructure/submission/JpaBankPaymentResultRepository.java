package com.corvian.payroll_payment_orchestrator.banks.infrastructure.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaBankPaymentResultRepository extends JpaRepository<BankPaymentResultEntity, UUID> {
    Optional<BankPaymentResultEntity> findBySubmissionIdAndPaymentId(UUID submissionId, UUID paymentId);
    List<BankPaymentResultEntity> findBySubmissionId(UUID submissionId);
}
