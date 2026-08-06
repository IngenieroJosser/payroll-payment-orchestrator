package com.corvian.payroll_payment_orchestrator.shared.deployment;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.stereotype.Component;

@Component
public class PaymentExecutionPolicy {
    private final DeploymentProperties properties;

    public PaymentExecutionPolicy(DeploymentProperties properties) {
        this.properties = properties;
    }

    public void requireEnabled() {
        if (!properties.isPaymentExecutionEnabled()) {
            throw new DomainException("PAYMENT_EXECUTION_DISABLED",
                    "Payment execution is disabled for this deployment. Complete bank certification and enable the controlled go-live flag.");
        }
    }
}
