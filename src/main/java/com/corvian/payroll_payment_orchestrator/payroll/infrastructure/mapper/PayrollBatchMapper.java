package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.mapper;

import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence.PayrollBatchEntity;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence.PayrollPaymentEntity;
import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.util.MaskingUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

        return new PayrollBatch(
                entity.getId(),
                entity.getCompanyId(),
                entity.getSourceAccountId(),
                entity.getCurrency(),
                entity.getScheduledDate(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getTotalPayments(),
                payments,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PayrollBatchEntity toEntity(PayrollBatch domain) {
        PayrollBatchEntity entity = new PayrollBatchEntity();
        entity.setId(domain.id());
        entity.setCompanyId(domain.companyId());
        entity.setSourceAccountId(domain.sourceAccountId());
        entity.setCurrency(domain.currency());
        entity.setScheduledDate(domain.scheduledDate());
        entity.setStatus(domain.status());
        entity.setTotalAmount(domain.totalAmount());
        entity.setTotalPayments(domain.totalPayments());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());

        List<PayrollPaymentEntity> paymentEntities = new ArrayList<>();
        for (PayrollPayment payment : domain.payments()) {
            PayrollPaymentEntity paymentEntity = toPaymentEntity(payment);
            paymentEntity.setBatch(entity);
            paymentEntities.add(paymentEntity);
        }
        entity.setPayments(paymentEntities);

        return entity;
    }

    private PayrollPayment toPaymentDomain(PayrollPaymentEntity entity) {
        return new PayrollPayment(
                entity.getId(),
                entity.getEmployeeDocumentType(),
                entity.getEmployeeDocumentNumber(),
                entity.getEmployeeFullName(),
                entity.getBankCode(),
                entity.getAccountType(),
                entity.getAccountNumber(),
                entity.getAmount(),
                entity.getStatus()
        );
    }

    private PayrollPaymentEntity toPaymentEntity(PayrollPayment domain) {
        PayrollPaymentEntity entity = new PayrollPaymentEntity();
        entity.setId(domain.id());
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
        return entity;
    }
}
