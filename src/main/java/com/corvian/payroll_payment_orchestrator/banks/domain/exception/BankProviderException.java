package com.corvian.payroll_payment_orchestrator.banks.domain.exception;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;

public class BankProviderException extends DomainException {
    private final boolean retryable;

    public BankProviderException(String code, String message, boolean retryable) {
        super(code, message);
        this.retryable = retryable;
    }

    public boolean isRetryable() { return retryable; }
}
