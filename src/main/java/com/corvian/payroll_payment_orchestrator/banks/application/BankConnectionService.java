package com.corvian.payroll_payment_orchestrator.banks.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.banks.domain.BankConnectionStatus;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.BankConnectionEntity;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.JpaBankConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class BankConnectionService {
    private final JpaBankConnectionRepository repository;
    private final AuditLogService auditLogService;

    public BankConnectionService(JpaBankConnectionRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public BankConnectionEntity create(UUID companyId, String bankCode, String baseUrl, String apiToken) {
        BankConnectionEntity entity = new BankConnectionEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId);
        entity.setBankCode(bankCode.trim());
        entity.setBaseUrl(baseUrl.trim());
        entity.setApiToken(apiToken);
        entity.setStatus(BankConnectionStatus.ACTIVE);
        entity.setCreatedAt(OffsetDateTime.now());
        BankConnectionEntity saved = repository.save(entity);
        auditLogService.record("BANK_CONNECTION_CREATED", "BANK_CONNECTION", saved.getId(), "Bank connection created for bank " + bankCode);
        return saved;
    }
}
