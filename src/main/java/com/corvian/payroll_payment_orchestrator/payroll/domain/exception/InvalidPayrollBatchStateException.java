package com.corvian.payroll_payment_orchestrator.payroll.domain.exception;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;

public class InvalidPayrollBatchStateException extends DomainException {
    public InvalidPayrollBatchStateException(PayrollBatchStatus currentStatus, String action) {
        super("INVALID_PAYROLL_BATCH_STATE", "Cannot " + action + " payroll batch with status: " + currentStatus);
    }
}
