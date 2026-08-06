package com.corvian.payroll_payment_orchestrator.banks.governance;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "app.bank.governance")
public class BankProviderGovernanceProperties {
    private boolean allowUncertifiedProviders;
    private Set<String> certifiedProviders = new LinkedHashSet<>();

    public boolean isAllowUncertifiedProviders() {
        return allowUncertifiedProviders;
    }

    public void setAllowUncertifiedProviders(boolean allowUncertifiedProviders) {
        this.allowUncertifiedProviders = allowUncertifiedProviders;
    }

    public Set<String> getCertifiedProviders() {
        return certifiedProviders;
    }

    public void setCertifiedProviders(Set<String> certifiedProviders) {
        this.certifiedProviders = certifiedProviders == null ? new LinkedHashSet<>() : new LinkedHashSet<>(certifiedProviders);
    }
}
