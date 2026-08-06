package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.shared.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PayrollBatchEntity getBatch() { return batch; }
    public void setBatch(PayrollBatchEntity batch) { this.batch = batch; }
    public String getEmployeeDocumentType() { return employeeDocumentType; }
    public void setEmployeeDocumentType(String employeeDocumentType) { this.employeeDocumentType = employeeDocumentType; }
    public String getEmployeeDocumentNumber() { return employeeDocumentNumber; }
    public void setEmployeeDocumentNumber(String employeeDocumentNumber) { this.employeeDocumentNumber = employeeDocumentNumber; }
    public String getEmployeeDocumentHash() { return employeeDocumentHash; }
    public void setEmployeeDocumentHash(String employeeDocumentHash) { this.employeeDocumentHash = employeeDocumentHash; }
    public String getEmployeeFullName() { return employeeFullName; }
    public void setEmployeeFullName(String employeeFullName) { this.employeeFullName = employeeFullName; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountNumberHash() { return accountNumberHash; }
    public void setAccountNumberHash(String accountNumberHash) { this.accountNumberHash = accountNumberHash; }
    public String getAccountNumberLast4() { return accountNumberLast4; }
    public void setAccountNumberLast4(String accountNumberLast4) { this.accountNumberLast4 = accountNumberLast4; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PayrollPaymentStatus getStatus() { return status; }
    public void setStatus(PayrollPaymentStatus status) { this.status = status; }
}
