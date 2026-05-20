package com.corvian.payroll_payment_orchestrator.payroll.application.port.in;

import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import java.util.UUID;

public interface ApprovePayrollBatchUseCase {
    PayrollBatch approve(UUID batchId);
    PayrollBatch reject(UUID batchId, String reason);
}
