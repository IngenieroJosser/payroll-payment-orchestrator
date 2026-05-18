package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.shared.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Convert;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_payments")
public class PayrollPaymentEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private PayrollBatchEntity batch;

    @Column(name = "employee_document_type", nullable = false, length = 20)
    private String employeeDocumentType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "employee_document_number", nullable = false, length = 2000)
    private String employeeDocumentNumber;

    @Column(name = "employee_document_hash", length = 128)
    private String employeeDocumentHash;

    @Column(name = "employee_full_name", nullable = false, length = 180)
    private String employeeFullName;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_number", nullable = false, length = 2000)
    private String accountNumber;

    @Column(name = "account_number_hash", length = 128)
    private String accountNumberHash;

    @Column(name = "account_number_last4", length = 4)
    private String accountNumberLast4;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PayrollPaymentStatus status;
}
