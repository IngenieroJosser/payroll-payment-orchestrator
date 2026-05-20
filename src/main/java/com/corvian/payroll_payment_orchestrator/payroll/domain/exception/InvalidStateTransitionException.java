package com.corvian.payroll_payment_orchestrator.payroll.domain.exception;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;

public class InvalidStateTransitionException extends DomainException {
    public InvalidStateTransitionException(String message) {
        super("INVALID_STATE_TRANSITION", message);
    }
}
