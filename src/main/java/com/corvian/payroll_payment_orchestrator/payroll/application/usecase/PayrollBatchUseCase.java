package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;

import java.util.UUID;

public interface PayrollBatchUseCase {
    PayrollBatch create(CreatePayrollBatchCommand command);
    PayrollBatch findById(UUID batchId);
    PayrollBatch validate(UUID batchId);
    PayrollBatch approve(UUID batchId);
    PayrollBatch execute(UUID batchId);
}
