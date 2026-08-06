package com.corvian.payroll_payment_orchestrator.payroll.domain.enums;

public enum PayrollPaymentStatus {
    PENDING,
    PROCESSING,
    SENT_TO_BANK,
    PAID,
    REJECTED,
    RETURNED,
    FAILED,
    CANCELLED
}
