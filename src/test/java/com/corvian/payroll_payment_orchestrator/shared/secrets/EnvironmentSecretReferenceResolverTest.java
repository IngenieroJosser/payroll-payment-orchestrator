package com.corvian.payroll_payment_orchestrator.shared.secrets;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentSecretReferenceResolverTest {

    @Test
    void resolvesApprovedEnvironmentReferenceWithoutPersistingTheSecret() {
        var environment = new MockEnvironment().withProperty("BANK_ACME_API_TOKEN", "resolved-secret");
        var resolver = new EnvironmentSecretReferenceResolver(environment);

        assertEquals("resolved-secret", resolver.resolve("env:BANK_ACME_API_TOKEN"));
    }

    @Test
    void rejectsUnsupportedOrMissingReferences() {
        var resolver = new EnvironmentSecretReferenceResolver(new MockEnvironment());

        assertThrows(DomainException.class, () -> resolver.resolve("vault:path/to/secret"));
        assertThrows(DomainException.class, () -> resolver.resolve("env:MISSING_SECRET"));
    }
}
