package com.corvian.payroll_payment_orchestrator.payroll.application.port.in;

import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;

public interface CreatePayrollBatchUseCase {
    PayrollBatch create(CreatePayrollBatchCommand command);
}
