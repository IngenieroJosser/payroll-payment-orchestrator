package com.corvian.payroll_payment_orchestrator.shared.config;

import com.corvian.payroll_payment_orchestrator.banks.governance.BankProviderGovernanceProperties;
import com.corvian.payroll_payment_orchestrator.shared.deployment.DeploymentProperties;
import com.corvian.payroll_payment_orchestrator.shared.outbound.OutboundUrlProperties;
import com.corvian.payroll_payment_orchestrator.shared.security.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationSecurityValidatorTest {

    @Test
    void rejectsReusedCryptographicKeys() {
        var fixture = fixture("test");
        String shared = "independent-secret-material-must-not-be-reused-123456";
        fixture.security().setJwtSecret(shared);

        var validator = validator(fixture, shared, "another-independent-hash-key-material-123456", false, false);

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void productionGoLiveRequiresDedicatedCertifiedProvider() {
        var fixture = fixture("prod");
        fixture.outbound().setAllowedHosts(List.of("bank.example.com"));
        fixture.security().setIpAllowlistEnabled(false);
        fixture.security().setMtlsEnabled(false);
        fixture.deployment().setPaymentExecutionEnabled(true);
        fixture.governance().setCertifiedProviders(Set.of());

        assertThrows(IllegalStateException.class,
                () -> validator(fixture, key("encryption"), key("hash"), false, false).validate());
    }

    @Test
    void productionRestrictedModeStartsWithIndependentSecretsAndControlledEgress() {
        var fixture = fixture("prod");
        fixture.outbound().setAllowedHosts(List.of("bank.example.com"));
        fixture.deployment().setPaymentExecutionEnabled(false);

        assertDoesNotThrow(() -> validator(fixture, key("encryption"), key("hash"), false, false).validate());
    }

    private static ApplicationSecurityValidator validator(
            Fixture fixture,
            String encryptionKey,
            String hashKey,
            boolean bootstrap,
            boolean sandboxFallback
    ) {
        return new ApplicationSecurityValidator(fixture.security(), fixture.environment(), fixture.outbound(),
                fixture.governance(), fixture.deployment(), encryptionKey, "", hashKey, bootstrap, "", "", sandboxFallback);
    }

    private static Fixture fixture(String profile) {
        var environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        var security = new SecurityProperties();
        security.setJwtSecret(key("jwt"));
        security.setJwtIssuer("issuer");
        security.setJwtAudience("audience");
        var outbound = new OutboundUrlProperties();
        var governance = new BankProviderGovernanceProperties();
        var deployment = new DeploymentProperties();
        return new Fixture(environment, security, outbound, governance, deployment);
    }

    private static String key(String purpose) {
        return purpose + "-cryptographic-material-abcdefghijklmnopqrstuvwxyz-0123456789";
    }

    private record Fixture(
            MockEnvironment environment,
            SecurityProperties security,
            OutboundUrlProperties outbound,
            BankProviderGovernanceProperties governance,
            DeploymentProperties deployment
    ) {}
}
