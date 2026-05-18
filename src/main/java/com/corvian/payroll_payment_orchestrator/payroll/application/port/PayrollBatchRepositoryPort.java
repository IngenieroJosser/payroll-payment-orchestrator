package com.corvian.payroll_payment_orchestrator.payroll.application.port;

import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollBatchRepositoryPort {
    PayrollBatch save(PayrollBatch payrollBatch);
    Optional<PayrollBatch> findById(UUID id);
    List<PayrollBatch> findAll();
}
