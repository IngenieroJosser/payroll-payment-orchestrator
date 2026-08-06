package com.corvian.payroll_payment_orchestrator.shared.config;

import com.corvian.payroll_payment_orchestrator.banks.governance.BankProviderGovernanceProperties;
import com.corvian.payroll_payment_orchestrator.shared.deployment.DeploymentProperties;
import com.corvian.payroll_payment_orchestrator.shared.outbound.OutboundUrlProperties;
import com.corvian.payroll_payment_orchestrator.shared.security.SecurityProperties;
import com.corvian.payroll_payment_orchestrator.shared.security.IpNetworkMatcher;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
public class ApplicationSecurityValidator {
    private static final String[] FORBIDDEN_MARKERS = {
            "CHANGE_ME", "CHANGEME", "REPLACE-WITH", "REPLACE_WITH", "EXAMPLE", "Admin123", "dev-token",
            "0123456789ABCDEF", "password", "secret123"
    };

    private final SecurityProperties securityProperties;
    private final Environment environment;
    private final OutboundUrlProperties outboundUrlProperties;
    private final BankProviderGovernanceProperties bankGovernanceProperties;
    private final DeploymentProperties deploymentProperties;
    private final String encryptionKey;
    private final String previousEncryptionKey;
    private final String hashKey;
    private final boolean bootstrapEnabled;
    private final String bootstrapEmail;
    private final String bootstrapPassword;
    private final boolean sandboxFallbackEnabled;

    public ApplicationSecurityValidator(
            SecurityProperties securityProperties,
            Environment environment,
            OutboundUrlProperties outboundUrlProperties,
            BankProviderGovernanceProperties bankGovernanceProperties,
            DeploymentProperties deploymentProperties,
            @Value("${app.crypto.encryption-key}") String encryptionKey,
            @Value("${app.crypto.previous-encryption-key:}") String previousEncryptionKey,
            @Value("${app.crypto.hash-key}") String hashKey,
            @Value("${app.bootstrap.enabled:false}") boolean bootstrapEnabled,
            @Value("${app.bootstrap.admin.email:}") String bootstrapEmail,
            @Value("${app.bootstrap.admin.password:}") String bootstrapPassword,
            @Value("${app.bank.sandbox-fallback-enabled:false}") boolean sandboxFallbackEnabled
    ) {
        this.securityProperties = securityProperties;
        this.environment = environment;
        this.outboundUrlProperties = outboundUrlProperties;
        this.bankGovernanceProperties = bankGovernanceProperties;
        this.deploymentProperties = deploymentProperties;
        this.encryptionKey = encryptionKey;
        this.previousEncryptionKey = previousEncryptionKey;
        this.hashKey = hashKey;
        this.bootstrapEnabled = bootstrapEnabled;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
        this.sandboxFallbackEnabled = sandboxFallbackEnabled;
    }

    @PostConstruct
    void validate() {
        boolean test = hasProfile("test");
        boolean development = hasProfile("dev");
        boolean production = hasProfile("prod");
        boolean staging = hasProfile("staging");

        validateLength("app.security.jwt-secret", securityProperties.getJwtSecret(), 32);
        validateLength("app.crypto.encryption-key", encryptionKey, 32);
        validateLength("app.crypto.hash-key", hashKey, 32);
        validateDistinctSecrets();
        validatePreviousEncryptionKey(test);
        validateSecurityRanges();
        validateIdentityMetadata();

        if (!test) {
            rejectKnownDefaults("app.security.jwt-secret", securityProperties.getJwtSecret());
            rejectKnownDefaults("app.crypto.encryption-key", encryptionKey);
            rejectKnownDefaults("app.crypto.hash-key", hashKey);
        }

        if (bootstrapEnabled) {
            if (!development && !test) {
                throw new IllegalStateException("Administrative bootstrap is restricted to dev/test profiles");
            }
            validateBootstrapCredentials();
        }

        if (!development && !test) {
            validateRestrictedEnvironment();
        }

        if (production) {
            if (sandboxFallbackEnabled) {
                throw new IllegalStateException("Sandbox bank fallback must be disabled in production");
            }
            if (bankGovernanceProperties.isAllowUncertifiedProviders()) {
                throw new IllegalStateException("Uncertified bank providers cannot be enabled in production");
            }
        }

        if ((production || staging) && deploymentProperties.isPaymentExecutionEnabled()) {
            validateGoLiveConfiguration();
        }
    }

    private void validateRestrictedEnvironment() {
        if (securityProperties.isIpAllowlistEnabled() && securityProperties.getIpAllowlist().isEmpty()) {
            throw new IllegalStateException("app.security.ip-allowlist must not be empty when IP allowlisting is enabled");
        }
        validateIpRules("app.security.ip-allowlist", securityProperties.getIpAllowlist());
        validateIpRules("app.security.trusted-proxy-addresses", securityProperties.getTrustedProxyAddresses());
        if (securityProperties.isMtlsEnabled() && securityProperties.getMtlsAllowedSubjects().isEmpty()) {
            throw new IllegalStateException("app.security.mtls-allowed-subjects must not be empty when mTLS is enabled");
        }
        if (outboundUrlProperties.isAllowHttp()) {
            throw new IllegalStateException("Outbound HTTP must be disabled outside dev/test profiles");
        }
        if (outboundUrlProperties.isAllowPrivateNetworks()) {
            throw new IllegalStateException("Outbound private-network access must be disabled outside dev/test profiles");
        }
        if (outboundUrlProperties.getAllowedHosts().isEmpty()) {
            throw new IllegalStateException("app.outbound.allowed-hosts must not be empty outside dev/test profiles");
        }
    }


    private void validateIpRules(String name, java.util.List<String> rules) {
        for (String rule : rules) {
            if (!IpNetworkMatcher.isValidRule(rule)) {
                throw new IllegalStateException(name + " contains an invalid IP/CIDR rule");
            }
        }
    }

    private void validateGoLiveConfiguration() {
        Set<String> certified = bankGovernanceProperties.getCertifiedProviders().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (certified.isEmpty()) {
            throw new IllegalStateException("Payment execution requires at least one certified bank provider");
        }
        if (certified.contains("SANDBOX") || certified.contains("REST_GENERIC")) {
            throw new IllegalStateException("SANDBOX and REST_GENERIC cannot be declared as certified providers");
        }
        if (bankGovernanceProperties.isAllowUncertifiedProviders()) {
            throw new IllegalStateException("Payment execution cannot be enabled while uncertified providers are allowed");
        }
    }


    private void validatePreviousEncryptionKey(boolean test) {
        if (previousEncryptionKey == null || previousEncryptionKey.isBlank()) return;
        validateLength("app.crypto.previous-encryption-key", previousEncryptionKey, 32);
        if (!test) rejectKnownDefaults("app.crypto.previous-encryption-key", previousEncryptionKey);
        if (previousEncryptionKey.equals(encryptionKey) || previousEncryptionKey.equals(hashKey)
                || previousEncryptionKey.equals(securityProperties.getJwtSecret())) {
            throw new IllegalStateException("Previous encryption key must be distinct from active cryptographic material");
        }
    }

    private void validateBootstrapCredentials() {
        if (bootstrapEmail == null || bootstrapEmail.isBlank() || !bootstrapEmail.contains("@")) {
            throw new IllegalStateException("A valid bootstrap administrator email is required when bootstrap is enabled");
        }
        validateLength("app.bootstrap.admin.password", bootstrapPassword, 16);
        rejectKnownDefaults("app.bootstrap.admin.password", bootstrapPassword);
    }

    private void validateIdentityMetadata() {
        if (securityProperties.getJwtIssuer() == null || securityProperties.getJwtIssuer().isBlank()) {
            throw new IllegalStateException("app.security.jwt-issuer must not be blank");
        }
        if (securityProperties.getJwtAudience() == null || securityProperties.getJwtAudience().isBlank()) {
            throw new IllegalStateException("app.security.jwt-audience must not be blank");
        }
    }

    private void validateDistinctSecrets() {
        String jwt = securityProperties.getJwtSecret();
        if (jwt.equals(encryptionKey) || jwt.equals(hashKey) || encryptionKey.equals(hashKey)) {
            throw new IllegalStateException("JWT, encryption and hash keys must be cryptographically independent");
        }
    }

    private void validateSecurityRanges() {
        if (securityProperties.getJwtExpirationMinutes() < 1 || securityProperties.getJwtExpirationMinutes() > 1_440) {
            throw new IllegalStateException("app.security.jwt-expiration-minutes must be between 1 and 1440");
        }
        if (securityProperties.getJwtClockSkewSeconds() < 0 || securityProperties.getJwtClockSkewSeconds() > 300) {
            throw new IllegalStateException("app.security.jwt-clock-skew-seconds must be between 0 and 300");
        }
        if (securityProperties.getRateLimitRequests() < 1 || securityProperties.getRateLimitWindowSeconds() < 1) {
            throw new IllegalStateException("Rate limit request and window values must be positive");
        }
    }

    private void validateLength(String name, String value, int minimumBytes) {
        if (value == null || value.isBlank() || value.getBytes(StandardCharsets.UTF_8).length < minimumBytes) {
            throw new IllegalStateException(name + " must contain at least " + minimumBytes + " bytes");
        }
    }

    private void rejectKnownDefaults(String name, String value) {
        String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
        for (String marker : FORBIDDEN_MARKERS) {
            if (normalized.contains(marker.toUpperCase(Locale.ROOT))) {
                throw new IllegalStateException(name + " contains an insecure placeholder or known default");
            }
        }
    }

    private boolean hasProfile(String profile) {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(profile::equals);
    }
}
