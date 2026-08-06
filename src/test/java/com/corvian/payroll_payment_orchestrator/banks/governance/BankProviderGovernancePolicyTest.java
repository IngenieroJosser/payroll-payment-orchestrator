package com.corvian.payroll_payment_orchestrator.banks.governance;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankProviderGovernancePolicyTest {

    @Test
    void productionRejectsReferenceAndSandboxProviders() {
        var properties = new BankProviderGovernanceProperties();
        properties.setCertifiedProviders(Set.of("BANK_ACME"));
        var policy = new BankProviderGovernancePolicy(properties, new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        assertThrows(DomainException.class, () -> policy.requireAllowed("REST_GENERIC"));
        assertThrows(DomainException.class, () -> policy.requireAllowed("SANDBOX"));
    }

    @Test
    void productionAllowsOnlyCertifiedProviders() {
        var properties = new BankProviderGovernanceProperties();
        properties.setCertifiedProviders(Set.of("BANK_ACME"));
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var policy = new BankProviderGovernancePolicy(properties, environment);

        assertDoesNotThrow(() -> policy.requireAllowed("BANK_ACME"));
        assertThrows(DomainException.class, () -> policy.requireAllowed("BANK_OTHER"));
    }

    @Test
    void developmentAllowsReferenceProvidersForLocalTesting() {
        var properties = new BankProviderGovernanceProperties();
        var environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        var policy = new BankProviderGovernancePolicy(properties, environment);

        assertDoesNotThrow(() -> policy.requireAllowed("REST_GENERIC"));
        assertDoesNotThrow(() -> policy.requireAllowed("SANDBOX"));
    }
}
