package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPayrollBatchStatusHistoryRepository extends JpaRepository<PayrollBatchStatusHistoryEntity, UUID> {
}
