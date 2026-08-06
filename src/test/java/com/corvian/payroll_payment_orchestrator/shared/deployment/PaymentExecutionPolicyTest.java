package com.corvian.payroll_payment_orchestrator.shared.deployment;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentExecutionPolicyTest {

    @Test
    void rejectsExecutionWhenDeploymentIsNotApprovedForGoLive() {
        var properties = new DeploymentProperties();
        properties.setPaymentExecutionEnabled(false);

        assertThrows(DomainException.class, () -> new PaymentExecutionPolicy(properties).requireEnabled());
    }

    @Test
    void allowsExecutionAfterControlledGoLiveFlagIsEnabled() {
        var properties = new DeploymentProperties();
        properties.setPaymentExecutionEnabled(true);

        assertDoesNotThrow(() -> new PaymentExecutionPolicy(properties).requireEnabled());
    }
}
