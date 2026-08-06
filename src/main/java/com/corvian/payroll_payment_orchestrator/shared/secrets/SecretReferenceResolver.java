package com.corvian.payroll_payment_orchestrator.shared.secrets;

public interface SecretReferenceResolver {
    String resolve(String reference);
}
