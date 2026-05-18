package com.corvian.payroll_payment_orchestrator.payroll.domain.enums;

public enum PayrollBatchStatus {
    DRAFT,
    VALIDATING,
    VALIDATED,
    PENDING_APPROVAL,
    APPROVED,
    SCHEDULED,
    PROCESSING,
    SENT_TO_BANK,
    PARTIALLY_PAID,
    PAID,
    FAILED,
    CANCELLED,
    REJECTED
}
