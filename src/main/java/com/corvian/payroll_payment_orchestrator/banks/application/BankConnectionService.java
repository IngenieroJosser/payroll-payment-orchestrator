package com.corvian.payroll_payment_orchestrator.banks.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.banks.application.model.BankConnectionProfile;
import com.corvian.payroll_payment_orchestrator.banks.domain.BankConnectionStatus;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.BankConnectionEntity;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.JpaBankConnectionRepository;
import com.corvian.payroll_payment_orchestrator.banks.governance.BankProviderGovernancePolicy;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.BankAccountEntity;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.JpaBankAccountRepository;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.outbound.OutboundUrlPolicy;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ResourceAccessService;
import com.corvian.payroll_payment_orchestrator.shared.secrets.SecretReferenceResolver;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class BankConnectionService {
    private final JpaBankConnectionRepository repository;
    private final JpaBankAccountRepository accountRepository;
    private final AuditLogService auditLogService;
    private final ResourceAccessService accessService;
    private final OutboundUrlPolicy outboundUrlPolicy;
    private final BankProviderGovernancePolicy governancePolicy;
    private final SecretReferenceResolver secretReferenceResolver;
    private final Environment environment;
    private final Clock clock;

    public BankConnectionService(
            JpaBankConnectionRepository repository,
            JpaBankAccountRepository accountRepository,
            AuditLogService auditLogService,
            ResourceAccessService accessService,
            OutboundUrlPolicy outboundUrlPolicy,
            BankProviderGovernancePolicy governancePolicy,
            SecretReferenceResolver secretReferenceResolver,
            Environment environment,
            Clock clock
    ) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.auditLogService = auditLogService;
        this.accessService = accessService;
        this.outboundUrlPolicy = outboundUrlPolicy;
        this.governancePolicy = governancePolicy;
        this.secretReferenceResolver = secretReferenceResolver;
        this.environment = environment;
        this.clock = clock;
    }

    @Transactional
    public BankConnectionEntity create(UUID companyId, String bankCode, String baseUrl, String apiToken) {
        return create(companyId, bankCode, "REST_GENERIC", "PRODUCTION", baseUrl, apiToken, null, 5_000, 30_000);
    }

    @Transactional
    public BankConnectionEntity create(UUID companyId, String bankCode, String providerKey, String environment,
                                       String baseUrl, String apiToken, String credentialReference,
                                       Integer connectTimeoutMs, Integer readTimeoutMs) {
        var company = accessService.requireCompanyAccess(companyId);
        String normalizedProvider = normalize(providerKey, "REST_GENERIC", 60);
        governancePolicy.requireAllowed(normalizedProvider);
        String normalizedEnvironment = normalize(environment, "PRODUCTION", 30);
        URI baseUri = "SANDBOX".equals(normalizedProvider)
                ? URI.create("https://sandbox.invalid") : outboundUrlPolicy.validate(baseUrl);
        boolean inlineCredential = apiToken != null && !apiToken.isBlank();
        boolean externalCredential = credentialReference != null && !credentialReference.isBlank();
        if (inlineCredential && externalCredential) {
            throw new DomainException("AMBIGUOUS_BANK_CREDENTIAL", "Provide either apiToken or credentialReference, not both");
        }
        if (!"SANDBOX".equals(normalizedProvider) && !inlineCredential && !externalCredential) {
            throw new DomainException("BANK_CREDENTIAL_REQUIRED", "A bank credential reference is required for the selected adapter");
        }
        if (!isDevelopmentOrTest() && inlineCredential) {
            throw new DomainException("INLINE_BANK_CREDENTIAL_FORBIDDEN",
                    "Staging and production bank credentials must be supplied through an external secret reference");
        }
        if (externalCredential) {
            validateCredentialReference(credentialReference);
        }
        validateTimeout(connectTimeoutMs, 100, 60_000, "connect timeout");
        validateTimeout(readTimeoutMs, 100, 300_000, "read timeout");
        String normalizedBankCode = normalize(bankCode, null, 40);
        repository.findFirstByCompanyIdAndBankCodeAndStatus(companyId, normalizedBankCode, BankConnectionStatus.ACTIVE)
                .ifPresent(existing -> { throw new DomainException("ACTIVE_BANK_CONNECTION_EXISTS", "An active connection already exists for this company and bank"); });

        OffsetDateTime now = OffsetDateTime.now(clock);
        BankConnectionEntity entity = new BankConnectionEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(company.getTenantId());
        entity.setCompanyId(companyId);
        entity.setBankCode(normalizedBankCode);
        entity.setProviderKey(normalizedProvider);
        entity.setEnvironment(normalizedEnvironment);
        entity.setBaseUrl(baseUri.toString());
        entity.setApiToken(inlineCredential ? apiToken.trim() : null);
        entity.setCredentialReference(externalCredential ? credentialReference.trim() : null);
        entity.setCredentialMode("SANDBOX".equals(normalizedProvider) ? "NONE"
                : externalCredential ? "EXTERNAL_REFERENCE" : "INLINE_ENCRYPTED");
        entity.setConnectTimeoutMs(connectTimeoutMs == null ? 5_000 : connectTimeoutMs);
        entity.setReadTimeoutMs(readTimeoutMs == null ? 30_000 : readTimeoutMs);
        entity.setStatus(BankConnectionStatus.ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        BankConnectionEntity saved = repository.save(entity);
        auditLogService.record("BANK_CONNECTION_CREATED", "BANK_CONNECTION", saved.getId(),
                "Bank connection profile created for " + normalizedBankCode + " using " + normalizedProvider,
                company.getTenantId(), companyId);
        return saved;
    }

    @Transactional(readOnly = true)
    public BankConnectionProfile resolveForSourceAccount(UUID companyId, UUID sourceAccountId) {
        var company = accessService.requireCompanyAccess(companyId);
        BankAccountEntity source = accountRepository.findByIdAndCompanyId(sourceAccountId, companyId)
                .orElseThrow(() -> new DomainException("BANK_ACCOUNT_NOT_FOUND", "Source bank account was not found for the company"));
        BankConnectionEntity connection = repository.findFirstByCompanyIdAndBankCodeAndStatus(
                        companyId, source.getBankCode(), BankConnectionStatus.ACTIVE)
                .orElseThrow(() -> new DomainException("BANK_CONNECTION_NOT_CONFIGURED", "No active bank connection is configured for the source account bank"));
        governancePolicy.requireAllowed(connection.getProviderKey());
        return toProfile(connection, company.getTenantId());
    }

    @Transactional(readOnly = true)
    public BankConnectionProfile getProfile(UUID connectionId) {
        BankConnectionEntity entity = repository.findById(connectionId)
                .orElseThrow(() -> new DomainException("BANK_CONNECTION_NOT_FOUND", "Bank connection was not found"));
        accessService.requireCompanyAccess(entity.getCompanyId());
        governancePolicy.requireAllowed(entity.getProviderKey());
        return toProfile(entity, entity.getTenantId());
    }

    private BankConnectionProfile toProfile(BankConnectionEntity entity, UUID tenantId) {
        URI uri = "SANDBOX".equalsIgnoreCase(entity.getProviderKey())
                ? URI.create(entity.getBaseUrl()) : outboundUrlPolicy.validate(entity.getBaseUrl());
        String credential = resolveCredential(entity);
        return new BankConnectionProfile(entity.getId(), tenantId, entity.getCompanyId(), entity.getBankCode(),
                entity.getProviderKey(), entity.getEnvironment(), uri, credential,
                entity.getConnectTimeoutMs(), entity.getReadTimeoutMs());
    }


    private String resolveCredential(BankConnectionEntity entity) {
        if ("SANDBOX".equalsIgnoreCase(entity.getProviderKey())) return null;
        if (entity.getCredentialReference() != null && !entity.getCredentialReference().isBlank()) {
            return secretReferenceResolver.resolve(entity.getCredentialReference());
        }
        if (!isDevelopmentOrTest()) {
            throw new DomainException("INLINE_BANK_CREDENTIAL_FORBIDDEN",
                    "Existing inline bank credentials cannot be used outside dev/test profiles");
        }
        if (entity.getApiToken() == null || entity.getApiToken().isBlank()) {
            throw new DomainException("BANK_CREDENTIAL_REQUIRED", "Bank credential is unavailable");
        }
        return entity.getApiToken();
    }

    private void validateCredentialReference(String reference) {
        if (!reference.matches("^env:[A-Z][A-Z0-9_]{2,127}$")) {
            throw new DomainException("INVALID_SECRET_REFERENCE", "Bank credential reference must use env:VARIABLE_NAME");
        }
    }

    private boolean isDevelopmentOrTest() {
        return java.util.Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("dev") || profile.equals("test"));
    }

    private String normalize(String value, String defaultValue, int max) {
        String normalized = value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || normalized.length() > max || !normalized.matches("^[A-Z0-9_.-]+$")) {
            throw new DomainException("INVALID_BANK_CONFIGURATION", "Bank configuration value is invalid");
        }
        return normalized;
    }

    private void validateTimeout(Integer value, int min, int max, String field) {
        if (value != null && (value < min || value > max)) {
            throw new DomainException("INVALID_BANK_TIMEOUT", field + " must be between " + min + " and " + max + " milliseconds");
        }
    }
}
