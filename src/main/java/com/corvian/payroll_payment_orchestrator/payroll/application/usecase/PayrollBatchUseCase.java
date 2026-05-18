package com.corvian.payroll_payment_orchestrator.payroll.application.usecase;

import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;

import java.util.List;
import java.util.UUID;

public interface PayrollBatchUseCase {
    PayrollBatch create(CreatePayrollBatchCommand command);
    List<PayrollBatch> findAll();
    PayrollBatch findById(UUID batchId);
    PayrollBatch validate(UUID batchId);
    PayrollBatch approve(UUID batchId);
    PayrollBatch reject(UUID batchId, String reason);
    PayrollBatch execute(UUID batchId);
    PayrollBatch markAsSentToBank(UUID batchId);
    PayrollBatch markAsPaid(UUID batchId);
    PayrollBatch markAsFailed(UUID batchId, String reason);
}
