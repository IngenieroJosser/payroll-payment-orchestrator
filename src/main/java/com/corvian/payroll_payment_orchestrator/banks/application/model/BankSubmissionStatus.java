package com.corvian.payroll_payment_orchestrator.banks.application.model;

public enum BankSubmissionStatus {
    PREPARED, SUBMITTING, ACCEPTED, PROCESSING, PARTIALLY_SETTLED, SETTLED, REJECTED, FAILED, UNKNOWN;

    public boolean terminal() {
        return this == SETTLED || this == REJECTED || this == FAILED;
    }
}
