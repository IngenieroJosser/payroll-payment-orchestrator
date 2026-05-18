package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPayrollBatchRepository extends JpaRepository<PayrollBatchEntity, UUID> {
}
