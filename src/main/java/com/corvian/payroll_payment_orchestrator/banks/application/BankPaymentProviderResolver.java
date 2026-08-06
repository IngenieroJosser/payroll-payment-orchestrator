package com.corvian.payroll_payment_orchestrator.banks.application;

import com.corvian.payroll_payment_orchestrator.banks.governance.BankProviderGovernancePolicy;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BankPaymentProviderResolver {
    private final Map<String, BankPaymentProvider> providers;
    private final BankProviderGovernancePolicy governancePolicy;

    public BankPaymentProviderResolver(List<BankPaymentProvider> providers, BankProviderGovernancePolicy governancePolicy) {
        this.governancePolicy = governancePolicy;
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> provider.providerKey().toUpperCase(Locale.ROOT), Function.identity(),
                (first, ignored) -> first));
    }

    public BankPaymentProvider resolve(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            throw new DomainException("BANK_PROVIDER_REQUIRED", "Bank provider key is required");
        }
        String normalizedProviderKey = providerKey.trim().toUpperCase(Locale.ROOT);
        governancePolicy.requireAllowed(normalizedProviderKey);
        BankPaymentProvider provider = providers.get(normalizedProviderKey);
        if (provider == null) {
            throw new DomainException("BANK_PROVIDER_NOT_CONFIGURED", "No bank adapter is registered for provider " + providerKey);
        }
        return provider;
    }
}
