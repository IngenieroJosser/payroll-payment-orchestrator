package com.corvian.payroll_payment_orchestrator.shared.deployment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.deployment")
public class DeploymentProperties {
    private boolean paymentExecutionEnabled;

    public boolean isPaymentExecutionEnabled() {
        return paymentExecutionEnabled;
    }

    public void setPaymentExecutionEnabled(boolean paymentExecutionEnabled) {
        this.paymentExecutionEnabled = paymentExecutionEnabled;
    }
}
