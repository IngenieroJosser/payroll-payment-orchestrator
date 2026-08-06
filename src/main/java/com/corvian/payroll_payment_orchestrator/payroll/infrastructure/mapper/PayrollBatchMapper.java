package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.mapper;

import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence.PayrollBatchEntity;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence.PayrollPaymentEntity;
import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.util.MaskingUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PayrollBatchMapper {
    private final CryptoService cryptoService;

    public PayrollBatchMapper(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public PayrollBatch toDomain(PayrollBatchEntity entity) {
        List<PayrollPayment> payments = entity.getPayments() == null
                ? List.of()
                : entity.getPayments().stream().map(this::toPaymentDomain).toList();
        return new PayrollBatch(entity.getId(), entity.getCompanyId(), entity.getSourceAccountId(),
                entity.getCurrency(), entity.getScheduledDate(), entity.getStatus(), entity.getTotalAmount(),
                entity.getTotalPayments(), payments, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public PayrollBatchEntity newEntity(PayrollBatch domain, UUID tenantId, String createdBy) {
        PayrollBatchEntity entity = new PayrollBatchEntity();
        entity.setId(domain.id());
        entity.setTenantId(tenantId);
        entity.setCreatedBy(createdBy);
        updateEntity(domain, entity);
        return entity;
    }

    public void updateEntity(PayrollBatch domain, PayrollBatchEntity entity) {
        entity.setCompanyId(domain.companyId());
        entity.setSourceAccountId(domain.sourceAccountId());
        entity.setCurrency(domain.currency());
        entity.setScheduledDate(domain.scheduledDate());
        entity.setStatus(domain.status());
        entity.setTotalAmount(domain.totalAmount());
        entity.setTotalPayments(domain.totalPayments());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());

        Map<UUID, PayrollPaymentEntity> existing = new HashMap<>();
        for (PayrollPaymentEntity paymentEntity : entity.getPayments()) {
            PayrollPaymentEntity duplicate = existing.put(paymentEntity.getId(), paymentEntity);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate payroll payment entity id: " + paymentEntity.getId());
            }
        }
        Set<UUID> incomingIds = new HashSet<>();
        for (PayrollPayment payment : domain.payments()) {
            incomingIds.add(payment.id());
        }
        entity.getPayments().removeIf(payment -> !incomingIds.contains(payment.getId()));
        for (PayrollPayment payment : domain.payments()) {
            PayrollPaymentEntity target = existing.get(payment.id());
            if (target == null) {
                target = new PayrollPaymentEntity();
                target.setId(payment.id());
                target.setBatch(entity);
                entity.getPayments().add(target);
            }
            updatePaymentEntity(payment, target);
        }
    }

    private PayrollPayment toPaymentDomain(PayrollPaymentEntity entity) {
        return new PayrollPayment(entity.getId(), entity.getEmployeeDocumentType(), entity.getEmployeeDocumentNumber(),
                entity.getEmployeeFullName(), entity.getBankCode(), entity.getAccountType(), entity.getAccountNumber(),
                entity.getAmount(), entity.getStatus());
    }

    private void updatePaymentEntity(PayrollPayment domain, PayrollPaymentEntity entity) {
        entity.setEmployeeDocumentType(domain.employeeDocumentType());
        entity.setEmployeeDocumentNumber(domain.employeeDocumentNumber());
        entity.setEmployeeDocumentHash(cryptoService.hmacSha256(domain.employeeDocumentNumber()));
        entity.setEmployeeFullName(domain.employeeFullName());
        entity.setBankCode(domain.bankCode());
        entity.setAccountType(domain.accountType());
        entity.setAccountNumber(domain.accountNumber());
        entity.setAccountNumberHash(cryptoService.hmacSha256(domain.accountNumber()));
        entity.setAccountNumberLast4(MaskingUtils.last4(domain.accountNumber()));
        entity.setAmount(domain.amount());
        entity.setStatus(domain.status());
    }
}
