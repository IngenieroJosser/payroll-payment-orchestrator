package com.corvian.payroll_payment_orchestrator.payroll.domain.exception;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;

import java.util.UUID;

public class PayrollBatchNotFoundException extends DomainException {
    public PayrollBatchNotFoundException(UUID batchId) {
        super("PAYROLL_BATCH_NOT_FOUND", "Payroll batch was not found: " + batchId);
    }
}
