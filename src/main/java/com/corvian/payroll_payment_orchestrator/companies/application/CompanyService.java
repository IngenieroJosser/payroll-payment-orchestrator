package com.corvian.payroll_payment_orchestrator.companies.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.*;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorType;
import com.corvian.payroll_payment_orchestrator.shared.security.context.AuthenticatedActor;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ResourceAccessService;
import com.corvian.payroll_payment_orchestrator.shared.util.MaskingUtils;
import com.corvian.payroll_payment_orchestrator.tenants.infrastructure.JpaTenantRepository;
import com.corvian.payroll_payment_orchestrator.tenants.infrastructure.TenantStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CompanyService {
    private final JpaCompanyRepository companyRepository;
    private final JpaBankAccountRepository bankAccountRepository;
    private final JpaTenantRepository tenantRepository;
    private final AuditLogService auditLogService;
    private final CryptoService cryptoService;
    private final ResourceAccessService accessService;

    public CompanyService(
            JpaCompanyRepository companyRepository,
            JpaBankAccountRepository bankAccountRepository,
            JpaTenantRepository tenantRepository,
            AuditLogService auditLogService,
            CryptoService cryptoService,
            ResourceAccessService accessService
    ) {
        this.companyRepository = companyRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.tenantRepository = tenantRepository;
        this.auditLogService = auditLogService;
        this.cryptoService = cryptoService;
        this.accessService = accessService;
    }

    @Transactional
    public CompanyEntity create(UUID tenantId, String legalName, String taxId, String currency) {
        accessService.requireTenantAccess(tenantId);
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException("TENANT_NOT_FOUND", "Tenant was not found"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new DomainException("TENANT_NOT_ACTIVE", "Company cannot be created under an inactive tenant");
        }
        String normalizedTaxId = taxId.trim().toUpperCase(Locale.ROOT);
        if (companyRepository.existsByTenantIdAndTaxIdIgnoreCase(tenantId, normalizedTaxId)) {
            throw new DomainException("COMPANY_TAX_ID_ALREADY_EXISTS", "A company with the same tax identifier already exists in the tenant");
        }
        String normalizedCurrency = normalizeCurrency(currency);
        OffsetDateTime now = OffsetDateTime.now();
        CompanyEntity entity = new CompanyEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setLegalName(legalName.trim());
        entity.setTaxId(normalizedTaxId);
        entity.setCurrency(normalizedCurrency);
        entity.setStatus(CompanyStatus.ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        CompanyEntity saved = companyRepository.save(entity);
        auditLogService.record("COMPANY_CREATED", "COMPANY", saved.getId(), "Company created", tenantId, saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CompanyEntity> findAll() {
        AuthenticatedActor actor = accessService.currentActor();
        if (actor.platformAdmin() || actor.actorType() == ActorType.SYSTEM) return companyRepository.findAll();
        if (actor.companyId() != null) return companyRepository.findById(actor.companyId()).stream().toList();
        if (actor.tenantId() != null) return companyRepository.findByTenantId(actor.tenantId());
        return List.of();
    }

    @Transactional(readOnly = true)
    public CompanyEntity findById(UUID id) {
        return accessService.requireCompanyAccess(id);
    }

    @Transactional
    public BankAccountEntity addBankAccount(UUID companyId, String bankCode, AccountType accountType, String accountNumber) {
        CompanyEntity company = accessService.requireCompanyAccess(companyId);
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new DomainException("COMPANY_NOT_ACTIVE", "Bank accounts cannot be added to an inactive company");
        }
        String normalizedAccount = normalizeAccountNumber(accountNumber);
        String hash = cryptoService.hmacSha256(normalizedAccount);
        if (bankAccountRepository.existsByCompanyIdAndAccountNumberHash(companyId, hash)) {
            throw new DomainException("BANK_ACCOUNT_ALREADY_EXISTS", "Bank account is already registered for the company");
        }
        BankAccountEntity entity = new BankAccountEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId);
        entity.setBankCode(bankCode.trim().toUpperCase(Locale.ROOT));
        entity.setAccountType(accountType);
        entity.setAccountNumber(normalizedAccount);
        entity.setAccountNumberHash(hash);
        entity.setAccountNumberMasked(MaskingUtils.maskAccount(normalizedAccount));
        entity.setAccountNumberLast4(MaskingUtils.last4(normalizedAccount));
        entity.setStatus(BankAccountStatus.ACTIVE);
        entity.setCreatedAt(OffsetDateTime.now());
        BankAccountEntity saved = bankAccountRepository.save(entity);
        auditLogService.record("BANK_ACCOUNT_CREATED", "BANK_ACCOUNT", saved.getId(), "Bank account registered", company.getTenantId(), companyId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BankAccountEntity> findBankAccounts(UUID companyId) {
        accessService.requireCompanyAccess(companyId);
        return bankAccountRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public BankAccountEntity requireActiveBankAccount(UUID companyId, UUID accountId) {
        accessService.requireCompanyAccess(companyId);
        BankAccountEntity account = bankAccountRepository.findByIdAndCompanyId(accountId, companyId)
                .orElseThrow(() -> new DomainException("BANK_ACCOUNT_NOT_FOUND", "Source bank account was not found for the company"));
        if (account.getStatus() != BankAccountStatus.ACTIVE) {
            throw new DomainException("BANK_ACCOUNT_NOT_ACTIVE", "Source bank account is not active");
        }
        return account;
    }

    private String normalizeCurrency(String value) {
        try {
            return Currency.getInstance(value.trim().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (Exception ex) {
            throw new DomainException("INVALID_CURRENCY", "Currency must be a valid ISO-4217 code");
        }
    }

    private String normalizeAccountNumber(String value) {
        String normalized = value == null ? "" : value.replaceAll("[\\s-]", "");
        if (!normalized.matches("^[A-Za-z0-9]{4,34}$")) {
            throw new DomainException("INVALID_ACCOUNT_NUMBER", "Account number must contain between 4 and 34 alphanumeric characters");
        }
        return normalized;
    }
}
