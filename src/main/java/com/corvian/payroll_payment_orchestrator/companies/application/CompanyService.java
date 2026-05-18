package com.corvian.payroll_payment_orchestrator.companies.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.BankAccountEntity;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.BankAccountStatus;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.CompanyEntity;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.CompanyStatus;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.JpaBankAccountRepository;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.JpaCompanyRepository;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.util.MaskingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {
    private final JpaCompanyRepository companyRepository;
    private final JpaBankAccountRepository bankAccountRepository;
    private final AuditLogService auditLogService;

    public CompanyService(JpaCompanyRepository companyRepository, JpaBankAccountRepository bankAccountRepository, AuditLogService auditLogService) {
        this.companyRepository = companyRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyEntity create(UUID tenantId, String legalName, String taxId, String currency) {
        OffsetDateTime now = OffsetDateTime.now();
        CompanyEntity entity = new CompanyEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setLegalName(legalName.trim());
        entity.setTaxId(taxId.trim());
        entity.setCurrency(currency.trim().toUpperCase());
        entity.setStatus(CompanyStatus.ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        CompanyEntity saved = companyRepository.save(entity);
        auditLogService.record("COMPANY_CREATED", "COMPANY", saved.getId(), "Company created: " + saved.getLegalName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CompanyEntity> findAll() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CompanyEntity findById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new DomainException("COMPANY_NOT_FOUND", "Company was not found"));
    }

    @Transactional
    public BankAccountEntity addBankAccount(UUID companyId, String bankCode, AccountType accountType, String accountNumber) {
        findById(companyId);
        BankAccountEntity entity = new BankAccountEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId);
        entity.setBankCode(bankCode.trim());
        entity.setAccountType(accountType);
        entity.setAccountNumberMasked(MaskingUtils.maskAccount(accountNumber));
        entity.setAccountNumberLast4(MaskingUtils.last4(accountNumber));
        entity.setStatus(BankAccountStatus.ACTIVE);
        entity.setCreatedAt(OffsetDateTime.now());
        BankAccountEntity saved = bankAccountRepository.save(entity);
        auditLogService.record("BANK_ACCOUNT_CREATED", "BANK_ACCOUNT", saved.getId(), "Bank account registered for company " + companyId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BankAccountEntity> findBankAccounts(UUID companyId) {
        findById(companyId);
        return bankAccountRepository.findByCompanyId(companyId);
    }
}
