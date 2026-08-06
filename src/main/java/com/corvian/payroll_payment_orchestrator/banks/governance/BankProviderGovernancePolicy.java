package com.corvian.payroll_payment_orchestrator.banks.governance;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BankProviderGovernancePolicy {
    private static final String SANDBOX = "SANDBOX";
    private static final String GENERIC_REFERENCE = "REST_GENERIC";

    private final BankProviderGovernanceProperties properties;
    private final Environment environment;

    public BankProviderGovernancePolicy(BankProviderGovernanceProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public void requireAllowed(String providerKey) {
        String normalized = normalize(providerKey);
        boolean developmentOrTest = hasAnyProfile("dev", "test");
        boolean production = hasAnyProfile("prod");

        if (SANDBOX.equals(normalized) && !developmentOrTest) {
            throw new DomainException("BANK_PROVIDER_NOT_PRODUCTION_ELIGIBLE",
                    "The sandbox bank provider is restricted to dev/test profiles");
        }
        if (GENERIC_REFERENCE.equals(normalized) && production) {
            throw new DomainException("BANK_PROVIDER_NOT_CERTIFIED",
                    "REST_GENERIC is a reference adapter and cannot execute production payments");
        }
        if (developmentOrTest || properties.isAllowUncertifiedProviders()) {
            return;
        }
        if (!certifiedProviders().contains(normalized)) {
            throw new DomainException("BANK_PROVIDER_NOT_CERTIFIED",
                    "Bank provider " + normalized + " is not included in the certified provider allowlist");
        }
    }

    public boolean isCertified(String providerKey) {
        String normalized = normalize(providerKey);
        return !SANDBOX.equals(normalized)
                && !GENERIC_REFERENCE.equals(normalized)
                && certifiedProviders().contains(normalized);
    }

    public Set<String> certifiedProviders() {
        return properties.getCertifiedProviders().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean hasAnyProfile(String... profiles) {
        Set<String> expected = Arrays.stream(profiles).collect(Collectors.toSet());
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(expected::contains);
    }

    private String normalize(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            throw new DomainException("BANK_PROVIDER_REQUIRED", "Bank provider key is required");
        }
        String normalized = providerKey.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z0-9_.-]{2,60}$")) {
            throw new DomainException("INVALID_BANK_PROVIDER", "Bank provider key is invalid");
        }
        return normalized;
    }
}
